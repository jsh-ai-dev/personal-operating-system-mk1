package com.jsh.pos.adapter.out.ai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.jsh.pos.application.port.out.AiSummaryException
import com.jsh.pos.application.port.out.AiSummaryPort
import com.jsh.pos.application.port.out.AiSummaryResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * Gemini API를 호출해 텍스트를 요약하는 어댑터입니다.
 *
 * 활성 조건:
 * - pos.ai.provider=gemini (또는 미설정 시 기본값)
 */
@Component
@ConditionalOnProperty(prefix = "pos.ai", name = ["provider"], havingValue = "gemini", matchIfMissing = true)
class GeminiSummaryAdapter(
    @Value("\${pos.ai.gemini.api-key:}") private val apiKey: String,
    @Value("\${pos.ai.gemini.flash-model:${GeminiModelResolver.DEFAULT_FLASH_MODEL}}") private val flashModel: String,
    @Value("\${pos.ai.gemini.pro-model:${GeminiModelResolver.DEFAULT_PRO_MODEL}}") private val proModel: String,
    @Value("\${pos.ai.gemini.max-output-tokens:8192}") private val maxOutputTokens: Int,
) : AiSummaryPort {

    private val restClient = RestClient.builder()
        .baseUrl("https://generativelanguage.googleapis.com")
        .build()

    override fun summarize(text: String, modelTier: String): AiSummaryResult {
        if (apiKey.isBlank()) {
            throw AiSummaryException(
                "Gemini API 키가 설정되지 않았습니다. " +
                    "GEMINI_API_KEY 환경변수를 설정하거나 application.yaml의 " +
                    "pos.ai.gemini.api-key 값을 지정해주세요."
            )
        }

        val model = resolveModelName(modelTier)

        val summary = requestSummary(
            text = text,
            model = model,
        )

        val result = summary ?: throw AiSummaryException(
            "Gemini 응답에서 요약 결과를 찾을 수 없습니다. " +
                "필요하면 max-output-tokens 값을 늘려보세요."
        )
        return AiSummaryResult(
            summary = result,
            modelTier = modelTier.trim().lowercase(),
        )
    }

    private fun requestSummary(
        text: String,
        model: String,
    ): String? {
        val requestBody = GeminiGenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = "$SYSTEM_PROMPT\n\n요약 대상 텍스트:\n$text")
                    )
                )
            ),
            generationConfig = GenerationConfig(
                maxOutputTokens = maxOutputTokens,
                temperature = 0.0,
            )
        )

        val response = try {
            restClient.post()
                .uri("/v1/models/$model:generateContent?key=$apiKey")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(GeminiGenerateContentResponse::class.java)
        } catch (e: HttpClientErrorException.NotFound) {
            throw AiSummaryException(
                "Gemini 모델 '$model' 을(를) generateContent로 찾을 수 없습니다. " +
                    "현재 키에서 지원되는 모델로 바꿔주세요. 예: " +
                    "${GeminiModelResolver.DEFAULT_FLASH_MODEL}, ${GeminiModelResolver.DEFAULT_PRO_MODEL}. " +
                    "필요하면 .env 의 GEMINI_FLASH_MODEL / GEMINI_PRO_MODEL 값을 수정한 뒤 서버를 다시 시작하세요.",
                e,
            )
        } catch (e: RestClientException) {
            throw AiSummaryException("Gemini API 호출 중 오류가 발생했습니다: ${e.message}", e)
        }

        if (GeminiResponseExtractor.extractFinishReason(response).equals("MAX_TOKENS", ignoreCase = true)) {
            throw AiSummaryException(
                "Gemini 요약이 출력 토큰 제한으로 중단되었습니다. " +
                    "GEMINI_MAX_OUTPUT_TOKENS 값을 늘리거나 더 짧은 원문으로 다시 시도하세요.",
            )
        }

        return GeminiResponseExtractor.extractText(response)
    }

    private fun resolveModelName(modelTier: String): String = GeminiModelResolver.resolve(
        modelTier = modelTier,
        flashModel = flashModel,
        proModel = proModel,
    )

    companion object {
        private val SYSTEM_PROMPT = """
            당신은 개발자의 학습/지식정리를 돕는 기술 요약 어시스턴트입니다.
            출력은 반드시 한국어 일반 텍스트로 작성하고, 아래 형식을 정확히 지키세요.

            주제 한줄:
            - 이 문서가 어떤 기술/개념에 대한 내용인지 한 줄로 설명

            왜 필요한가:
            - 이 기술을 왜 써야 하는지 2~4개 bullet
            - 기존 방식의 단점과 이 기술이 보완하는 지점을 함께 설명

            한눈에 요약:
            - 핵심만 4~6개 bullet

            핵심 개념 정리:
            - 개념명: 설명 형식으로 3~7개 bullet

            실무 적용 포인트:
            - 바로 적용 가능한 체크리스트 3~5개 bullet

            기술면접 예상 질문과 답변:
            - 질문: 형식으로 2~3개
            - 답변: 각 질문에 대해 2~4문장으로 핵심만 설명

            규칙:
            1. 강조 마크다운(**, `, _, ~)과 코드 펜스는 사용하지 않습니다.
            2. 목록은 모두 '- '로 시작합니다.
            3. 원문에 없는 내용을 단정하지 말고, 추론이 필요하면 '(추론)' 표시를 붙입니다.
            4. 장황한 서론/결론은 금지하고 본문만 출력합니다.
        """.trimIndent()

    }
}

internal object GeminiModelResolver {
    internal const val DEFAULT_FLASH_MODEL = "gemini-2.5-flash"
    internal const val DEFAULT_PRO_MODEL = "gemini-2.5-pro"

    fun resolve(modelTier: String, flashModel: String, proModel: String): String {
        val configuredModel = when (modelTier.trim().lowercase()) {
            "pro" -> proModel
            "flash" -> flashModel
            else -> flashModel
        }

        return normalize(configuredModel).ifBlank {
            normalize(
                if (modelTier.trim().lowercase() == "pro") DEFAULT_PRO_MODEL else DEFAULT_FLASH_MODEL
            )
        }
    }

    private fun normalize(modelName: String): String = modelName
        .trim()
        .removePrefix("models/")
}

private data class GeminiGenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig,
)

private data class Content(
    val parts: List<Part>,
)

private data class Part(
    val text: String,
)

private data class GenerationConfig(
    @com.fasterxml.jackson.annotation.JsonProperty("maxOutputTokens")
    val maxOutputTokens: Int,
    val temperature: Double,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class GeminiGenerateContentResponse(
    val candidates: List<Candidate> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Candidate(
    val content: CandidateContent? = null,
    val finishReason: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CandidateContent(
    val parts: List<CandidatePart> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CandidatePart(
    val text: String? = null,
)

internal object GeminiResponseExtractor {

    fun extractFinishReason(response: GeminiGenerateContentResponse?): String? =
        response?.candidates?.firstOrNull()?.finishReason

    fun extractText(response: GeminiGenerateContentResponse?): String? {
        val candidate = response?.candidates?.firstOrNull()
        return candidate?.content
            ?.parts
            .orEmpty()
            .mapNotNull { it.text }
            .joinToString(separator = "")
            .trim()
            .takeIf { it.isNotBlank() }
    }
}

