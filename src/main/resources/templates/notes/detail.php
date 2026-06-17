<?php
/**
 * PHP version of notes/detail.html for comparison only.
 *
 * This file is not wired into the Spring Boot app. Thymeleaf resolves
 * notes/detail to notes/detail.html, so this .php file should not affect runtime.
 *
 * Expected PHP view variables:
 * $note, $tagsDisplay, $message, $selectedSummaryModelTier, $generatedSummary,
 * $csrfParameterName, $csrfToken
 */

function detail_e(mixed $value): string
{
    return htmlspecialchars((string) ($value ?? ''), ENT_QUOTES, 'UTF-8');
}

function detail_field(mixed $value, string $name, mixed $default = null): mixed
{
    if (is_array($value)) {
        return $value[$name] ?? $default;
    }

    if (is_object($value)) {
        return $value->{$name} ?? $default;
    }

    return $default;
}

$note = $note ?? [];
$tagsDisplay = $tagsDisplay ?? '-';
$message = $message ?? null;
$selectedSummaryModelTier = $selectedSummaryModelTier ?? 'gpt-5-nano';
$generatedSummary = $generatedSummary ?? null;
$csrfParameterName = $csrfParameterName ?? '_csrf';
$csrfToken = $csrfToken ?? '';

$noteId = (string) detail_field($note, 'id');
$hasStoredFile = (bool) detail_field($note, 'hasStoredFile', false);
$originalFileName = detail_field($note, 'originalFileName');
$fileContentType = detail_field($note, 'fileContentType');
$bookmarked = (bool) detail_field($note, 'bookmarked', false);
$aiSummary = detail_field($note, 'aiSummary');
?>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>노트 상세</title>
    <link rel="stylesheet" href="/css/notes.css">
    <script defer src="/js/notes-theme.js"></script>
</head>
<body>
<div class="site-shell">
    <header class="site-header">
        <a class="brand" href="/">Personal OS</a>
        <nav class="top-nav">
            <div class="nav-group">
                <a class="nav-link" href="/notes">노트 목록</a>
                <a class="nav-link nav-link-primary" href="/notes/new">새 노트</a>
                <a class="nav-link" href="/summary">AI 요약</a>
            </div>
            <button id="themeToggle" class="btn btn-ghost" type="button" aria-label="테마 전환">다크모드</button>
        </nav>
    </header>
</div>

<main class="site-shell page-stack">
    <a class="btn btn-ghost" href="/notes">목록으로</a>

    <article class="panel detail-card">
        <h1><?= detail_e(detail_field($note, 'title', '제목')) ?></h1>

        <?php if ($message): ?>
            <div class="message"><?= detail_e($message) ?></div>
        <?php endif; ?>

        <div class="meta">
            <span class="badge"><?= detail_e(detail_field($note, 'visibility', 'PRIVATE')) ?></span>
            <span>태그: <strong><?= detail_e($tagsDisplay) ?></strong></span>
            <span>북마크: <strong><?= $bookmarked ? 'ON' : 'OFF' ?></strong></span>
            <?php if ($originalFileName !== null): ?>
                <span>원본 파일: <strong><?= detail_e($originalFileName) ?></strong></span>
            <?php endif; ?>
            <?php if ($hasStoredFile && $fileContentType !== null): ?>
                <span>형식: <strong><?= detail_e($fileContentType) ?></strong></span>
            <?php endif; ?>
        </div>

        <?php if (!$hasStoredFile): ?>
            <pre class="note-pre"><?= detail_e(detail_field($note, 'content')) ?></pre>
        <?php else: ?>
            <div class="panel">
                이 파일 노트는 브라우저 내 미리보기를 지원하지 않습니다. 아래 다운로드 버튼으로 원본 파일을 내려받아 확인해주세요.
            </div>
        <?php endif; ?>

        <div class="actions">
            <?php if (!$hasStoredFile): ?>
                <a class="btn" href="/notes/<?= detail_e($noteId) ?>/edit">수정</a>
            <?php endif; ?>

            <?php if ($originalFileName !== null): ?>
                <a class="btn btn-ghost" href="/notes/<?= detail_e($noteId) ?>/download">다운로드</a>
            <?php endif; ?>

            <?php if (!$bookmarked): ?>
                <form action="/notes/<?= detail_e($noteId) ?>/bookmark" method="post">
                    <input type="hidden" name="<?= detail_e($csrfParameterName) ?>" value="<?= detail_e($csrfToken) ?>">
                    <button class="btn" type="submit">북마크</button>
                </form>
            <?php endif; ?>

            <?php if ($bookmarked): ?>
                <form action="/notes/<?= detail_e($noteId) ?>/unbookmark" method="post">
                    <input type="hidden" name="<?= detail_e($csrfParameterName) ?>" value="<?= detail_e($csrfToken) ?>">
                    <button class="btn btn-ghost" type="submit">북마크 해제</button>
                </form>
            <?php endif; ?>

            <form action="/notes/<?= detail_e($noteId) ?>/delete" method="post" onsubmit="return confirm('정말 삭제할까요?');">
                <input type="hidden" name="<?= detail_e($csrfParameterName) ?>" value="<?= detail_e($csrfToken) ?>">
                <button class="btn btn-danger" type="submit">삭제</button>
            </form>
        </div>

        <section class="panel detail-card">
            <h2>AI 요약</h2>

            <?php if ($aiSummary !== null): ?>
                <div class="summary-box"><?= detail_e($aiSummary) ?></div>
            <?php else: ?>
                <p class="subtle">아직 저장된 요약이 없습니다.</p>
            <?php endif; ?>

            <form class="actions" action="/notes/<?= detail_e($noteId) ?>/summary/generate" method="post">
                <input type="hidden" name="<?= detail_e($csrfParameterName) ?>" value="<?= detail_e($csrfToken) ?>">
                <select class="input input-inline" name="modelTier">
                    <option value="gpt-5-nano" <?= $selectedSummaryModelTier === 'gpt-5-nano' ? 'selected' : '' ?>>gpt-5-nano ($0.05 / $0.4)</option>
                    <option value="gpt-5-mini" <?= $selectedSummaryModelTier === 'gpt-5-mini' ? 'selected' : '' ?>>gpt-5-mini ($0.25 / $2)</option>
                    <option value="gpt-5" disabled>gpt-5 ($1.25 / $10)</option>
                </select>
                <button class="btn btn-primary" type="submit">AI 요약 생성</button>
            </form>

            <?php if ($generatedSummary !== null): ?>
                <form action="/notes/<?= detail_e($noteId) ?>/summary/save" method="post" class="page-stack">
                    <input type="hidden" name="<?= detail_e($csrfParameterName) ?>" value="<?= detail_e($csrfToken) ?>">
                    <label for="summary">생성된 요약 (저장 전)</label>
                    <textarea id="summary" name="summary" class="input" rows="12"><?= detail_e($generatedSummary) ?></textarea>
                    <div class="actions">
                        <button class="btn btn-primary" type="submit">요약 저장</button>
                    </div>
                </form>
            <?php endif; ?>
        </section>
    </article>
</main>
</body>
</html>
