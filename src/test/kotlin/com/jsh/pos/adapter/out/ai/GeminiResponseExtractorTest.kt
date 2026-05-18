package com.jsh.pos.adapter.out.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GeminiResponseExtractorTest {

    @Test
    fun `multiple response parts are joined into one summary`() {
        val response = GeminiGenerateContentResponse(
            candidates = listOf(
                Candidate(
                    content = CandidateContent(
                        parts = listOf(
                            CandidatePart(text = "- 코틀린은 불변 변수(val)와 강력한 Null 안정성 설계를 통해 자바 대비 런타임 에러를 줄입니다.\n"),
                            CandidatePart(text = "- data class, 문자열 템플릿, when 등을 통해 보일러플레이트를 줄여 생산성을 높입니다.\n"),
                            CandidatePart(text = "- Clean Architecture와 TDD 관점에서도 Kotlin은 의도를 더 선명하게 드러내기 좋습니다."),
                        )
                    )
                )
            )
        )

        val actual = GeminiResponseExtractor.extractText(response)

        assertEquals(
            "- 코틀린은 불변 변수(val)와 강력한 Null 안정성 설계를 통해 자바 대비 런타임 에러를 줄입니다.\n" +
                "- data class, 문자열 템플릿, when 등을 통해 보일러플레이트를 줄여 생산성을 높입니다.\n" +
                "- Clean Architecture와 TDD 관점에서도 Kotlin은 의도를 더 선명하게 드러내기 좋습니다.",
            actual,
        )
    }

    @Test
    fun `empty parts returns null`() {
        val response = GeminiGenerateContentResponse(
            candidates = listOf(
                Candidate(
                    content = CandidateContent(parts = emptyList()),
                )
            )
        )

        val actual = GeminiResponseExtractor.extractText(response)

        assertNull(actual)
    }

    @Test
    fun `empty response returns null`() {
        assertNull(GeminiResponseExtractor.extractText(null))
    }

    @Test
    fun `finish reason is extracted from first candidate`() {
        val response = GeminiGenerateContentResponse(
            candidates = listOf(
                Candidate(
                    finishReason = "MAX_TOKENS",
                    content = CandidateContent(parts = listOf(CandidatePart(text = "partial summary"))),
                )
            )
        )

        assertEquals("MAX_TOKENS", GeminiResponseExtractor.extractFinishReason(response))
    }
}

