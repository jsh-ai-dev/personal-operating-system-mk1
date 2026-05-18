package com.jsh.pos.adapter.out.ai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.jsh.pos.application.port.out.AiSummaryException
import com.jsh.pos.application.port.out.AiSummaryPort
import com.jsh.pos.application.port.out.AiSummaryResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.math.BigDecimal
import java.math.RoundingMode

@Component
@ConditionalOnProperty(prefix = "pos.ai", name = ["provider"], havingValue = "openai")
class OpenAiSummaryAdapter(
    @Value("\${pos.ai.openai.api-key:}") private val apiKey: String,
    @Value("\${pos.ai.openai.model:gpt-5-nano}") private val model: String,
    @Value("\${pos.ai.openai.max-tokens:8192}") private val maxTokens: Int,
) : AiSummaryPort {

    private val restClient = RestClient.builder()
        .baseUrl("https://api.openai.com")
        .build()

    override fun summarize(text: String, modelTier: String): AiSummaryResult {
        if (apiKey.isBlank()) {
            throw AiSummaryException("OpenAI API key is not configured. Set OPENAI_API_KEY or pos.ai.openai.api-key.")
        }

        val resolvedModel = resolveModelName(modelTier)
        val requestBody = ChatRequest(
            model = resolvedModel,
            messages = listOf(
                Message(role = "system", content = SYSTEM_PROMPT),
                Message(role = "user", content = text),
            ),
            maxTokens = maxTokens,
            reasoningEffort = "minimal",
        )

        val response = try {
            restClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(ChatResponse::class.java)
        } catch (e: RestClientException) {
            throw AiSummaryException("OpenAI API call failed: ${e.message}", e)
        }

        val choice = response?.choices?.firstOrNull()
            ?: throw AiSummaryException("OpenAI response did not include a summary.")
        val summary = choice.message.content?.trim()?.takeIf { it.isNotBlank() }
            ?: throw AiSummaryException(
                "OpenAI response was empty. finish_reason=${choice.finishReason ?: "unknown"}, " +
                    "increase OPENAI_MAX_TOKENS or try again.",
            )

        if (choice.finishReason?.lowercase() in setOf("length", "max_tokens")) {
            throw AiSummaryException(
                "OpenAI 요약이 출력 토큰 제한으로 중단되었습니다. " +
                    "OPENAI_MAX_TOKENS 값을 늘리거나 더 짧은 원문으로 다시 시도하세요.",
            )
        }

        return AiSummaryResult(
            summary = summary,
            modelTier = resolvedModel,
            inputTokens = response.usage?.promptTokens,
            outputTokens = response.usage?.completionTokens,
            estimatedCostUsd = estimateCostUsd(
                model = resolvedModel,
                inputTokens = response.usage?.promptTokens,
                outputTokens = response.usage?.completionTokens,
            ),
        )
    }

    private fun resolveModelName(modelTier: String): String {
        val normalized = modelTier.trim().lowercase()
        if (normalized in ALLOWED_MODELS) return normalized
        return model.trim().lowercase().takeIf { it in ALLOWED_MODELS } ?: DEFAULT_MODEL
    }

    private fun estimateCostUsd(model: String, inputTokens: Int?, outputTokens: Int?): BigDecimal? {
        val price = MODEL_PRICES[model] ?: return null
        if (inputTokens == null || outputTokens == null) return null
        val inputCost = BigDecimal(inputTokens)
            .multiply(price.inputPerMillion)
            .divide(ONE_MILLION, 12, RoundingMode.HALF_UP)
        val outputCost = BigDecimal(outputTokens)
            .multiply(price.outputPerMillion)
            .divide(ONE_MILLION, 12, RoundingMode.HALF_UP)
        return inputCost.add(outputCost).setScale(8, RoundingMode.HALF_UP)
    }

    private companion object {
        private const val DEFAULT_MODEL = "gpt-5-nano"
        private val ALLOWED_MODELS = setOf("gpt-5-nano", "gpt-5-mini")
        private val ONE_MILLION = BigDecimal("1000000")
        private val MODEL_PRICES = mapOf(
            "gpt-5-nano" to ModelPrice(inputPerMillion = BigDecimal("0.05"), outputPerMillion = BigDecimal("0.4")),
            "gpt-5-mini" to ModelPrice(inputPerMillion = BigDecimal("0.25"), outputPerMillion = BigDecimal("2")),
        )

        private val SYSTEM_PROMPT = """
            당신은 개발자의 학습/지식정리를 돕는 기술 요약 어시스턴트입니다.
            출력은 한국어 일반 텍스트로 작성하고, 과장하거나 원문에 없는 내용을 단정하지 마세요.

            제목은 숫자로, 목록은 '- '로 시작합니다.
            강한 마크다운, 코드블록, 결론 문구는 쓰지 않습니다.
            독자에게 추가 요청을 권하거나 예시 코드, 구성 예시, 스크립트 제공을 제안하는 채팅식 문구는 쓰지 않습니다.
            원문 요약만 출력하고, 어시스턴트의 후속 행동 제안은 출력하지 않습니다.

            1. 주제:
            - 이 문서가 어떤 기술/개념에 대한 내용인지 한 줄로 설명

            2. 핵심 요약:
            - 핵심만 요약해서 이어지는 3~4문장으로 설명

            3. 왜 필요한가:
            - 이 기술을 왜 써야 하는지 2~3개 bullet
            - 기존 방식의 단점과 이 기술이 보완하는 지점을 함께 설명

            4. 핵심 개념 정리:
            - 개념명: 설명 형식으로 3~7개 bullet

            5. 실무 적용 포인트:
            - 바로 적용 가능한 체크리스트 2~3개 bullet

            6. 기술면접 예상 질문과 답변:
            - 질문: 형식으로 2~3개
            - 답변: 각 질문에 대해 2~3문장으로 핵심만 설명
        """.trimIndent()
    }
}

private data class ModelPrice(
    val inputPerMillion: BigDecimal,
    val outputPerMillion: BigDecimal,
)

private data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    @JsonProperty("max_completion_tokens")
    val maxTokens: Int,
    @JsonProperty("reasoning_effort")
    val reasoningEffort: String,
)

private data class Message(
    val role: String,
    val content: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ChatResponse(
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Choice(
        val message: Message,
        @JsonProperty("finish_reason")
        val finishReason: String? = null,
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class Usage(
    @JsonProperty("prompt_tokens")
    val promptTokens: Int? = null,
    @JsonProperty("completion_tokens")
    val completionTokens: Int? = null,
)
