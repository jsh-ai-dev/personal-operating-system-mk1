<?php
/**
 * PHP version of notes/list.html for comparison only.
 *
 * This file is not wired into the Spring Boot app. Thymeleaf resolves
 * notes/list to notes/list.html, so this .php file should not affect runtime.
 *
 * Expected PHP view variables:
 * $notes, $keyword, $bookmarkedOnly, $sort, $page, $size, $totalElements,
 * $totalPages, $hasPrevious, $hasNext, $currentPageDisplay, $pageNumbers,
 * $highlightsById, $tagsDisplayById, $createdAtDisplayById, $updatedAtDisplayById,
 * $message, $csrfParameterName, $csrfToken
 */

function e(mixed $value): string
{
    return htmlspecialchars((string) ($value ?? ''), ENT_QUOTES, 'UTF-8');
}

function field(mixed $value, string $name, mixed $default = null): mixed
{
    if (is_array($value)) {
        return $value[$name] ?? $default;
    }

    if (is_object($value)) {
        return $value->{$name} ?? $default;
    }

    return $default;
}

function mapValue(array $map, string $key, mixed $default = null): mixed
{
    return $map[$key] ?? $default;
}

function notesUrl(array $params = []): string
{
    return '/notes' . ($params === [] ? '' : '?' . http_build_query($params));
}

$notes = $notes ?? [];
$keyword = $keyword ?? '';
$bookmarkedOnly = $bookmarkedOnly ?? false;
$sort = $sort ?? 'created';
$page = $page ?? 0;
$size = $size ?? 20;
$totalElements = $totalElements ?? 0;
$totalPages = $totalPages ?? 0;
$hasPrevious = $hasPrevious ?? false;
$hasNext = $hasNext ?? false;
$currentPageDisplay = $currentPageDisplay ?? ($page + 1);
$pageNumbers = $pageNumbers ?? range(0, max(0, $totalPages - 1));
$highlightsById = $highlightsById ?? [];
$tagsDisplayById = $tagsDisplayById ?? [];
$createdAtDisplayById = $createdAtDisplayById ?? [];
$updatedAtDisplayById = $updatedAtDisplayById ?? [];
$message = $message ?? null;
$csrfParameterName = $csrfParameterName ?? '_csrf';
$csrfToken = $csrfToken ?? '';
?>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>노트 목록</title>
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
    <section class="page-title-row">
        <div>
            <h1>노트 목록</h1>
            <p class="subtle">검색과 북마크 필터로 필요한 노트를 빠르게 찾을 수 있어요.</p>
        </div>
        <div class="btn-group">
            <a class="btn btn-primary" href="/notes/new">새 노트 만들기</a>
            <form method="post" action="/notes/upload" enctype="multipart/form-data" class="upload-form">
                <input type="hidden" name="<?= e($csrfParameterName) ?>" value="<?= e($csrfToken) ?>">
                <label class="btn btn-ghost upload-label" title=".txt 또는 .pdf 파일을 노트로 가져옵니다">
                    파일 가져오기
                    <input type="file" name="file" accept=".txt,.pdf,text/plain,application/pdf" class="upload-input" onchange="this.form.submit()">
                </label>
            </form>
        </div>
    </section>

    <?php if ($message): ?>
        <div class="message"><?= e($message) ?></div>
    <?php endif; ?>

    <form method="get" action="/notes" class="panel search-row">
        <input class="input" type="text" name="keyword" placeholder="제목/본문 검색" value="<?= e($keyword) ?>">
        <input type="hidden" name="page" value="0">
        <input type="hidden" name="size" value="<?= e($size) ?>">
        <select class="input input-inline" name="sort">
            <option value="created" <?= $sort === 'created' ? 'selected' : '' ?>>작성일순</option>
            <option value="recent" <?= $sort === 'recent' ? 'selected' : '' ?>>수정일순</option>
            <option value="title" <?= $sort === 'title' ? 'selected' : '' ?>>제목순</option>
            <option value="relevance" <?= $sort === 'relevance' ? 'selected' : '' ?>>관련도순</option>
        </select>
        <label class="checkbox-inline">
            <input type="checkbox" name="bookmarkedOnly" value="true" <?= $bookmarkedOnly ? 'checked' : '' ?>>
            북마크만 보기
        </label>
        <button class="btn" type="submit">검색</button>
        <a class="btn btn-ghost" href="/notes">초기화</a>
    </form>

    <?php if (count($notes) === 0): ?>
        <div class="panel empty">
            아직 노트가 없습니다. 첫 노트를 작성해보세요.
        </div>
    <?php endif; ?>

    <?php if (count($notes) > 0): ?>
        <section class="note-grid">
            <?php foreach ($notes as $note): ?>
                <?php
                $id = (string) field($note, 'id');
                $title = field($note, 'title');
                $hasStoredFile = (bool) field($note, 'hasStoredFile', false);
                $originalFileName = field($note, 'originalFileName');
                $isTxtUpload = $originalFileName !== null && !$hasStoredFile && str_ends_with(strtolower((string) $originalFileName), '.txt');
                $highlights = mapValue($highlightsById, $id, null);
                ?>
                <article class="note-card">
                    <div class="note-head">
                        <h2><a href="/notes/<?= e($id) ?>"><?= e($title) ?></a></h2>
                        <div class="note-card-badges">
                            <?php if ($hasStoredFile): ?>
                                <span class="file-badge" title="원본 파일 저장됨">PDF</span>
                            <?php endif; ?>
                            <?php if ($isTxtUpload): ?>
                                <span class="file-badge file-badge-txt" title="텍스트 파일 업로드 노트">TXT</span>
                            <?php endif; ?>
                            <?php if (field($note, 'aiSummary') !== null): ?>
                                <span class="summary-badge" title="AI 요약 저장됨">요약</span>
                            <?php endif; ?>
                            <?php if ((bool) field($note, 'bookmarked', false)): ?>
                                <span class="bookmark" title="북마크됨">*</span>
                            <?php endif; ?>
                        </div>
                    </div>

                    <?php if (!$hasStoredFile): ?>
                        <p class="note-content"><?= e(field($note, 'content')) ?></p>
                    <?php else: ?>
                        <p class="note-content">PDF 파일 노트입니다. 상세 화면에서 다운로드할 수 있습니다.</p>
                    <?php endif; ?>

                    <div class="meta">
                        <span class="badge"><?= e(field($note, 'visibility')) ?></span>
                        <?php if ($originalFileName !== null): ?>
                            <span>파일: <strong><?= e($originalFileName) ?></strong></span>
                        <?php endif; ?>
                        <span><?= e(mapValue($tagsDisplayById, $id, '-')) ?></span>
                        <span>생성: <strong><?= e(mapValue($createdAtDisplayById, $id, '-')) ?></strong></span>
                        <span>수정: <strong><?= e(mapValue($updatedAtDisplayById, $id, '-')) ?></strong></span>
                    </div>

                    <?php if ($highlights !== null): ?>
                        <div class="search-snippet">
                            <?php if (field($highlights, 'title') !== null): ?>
                                <p>제목: <span><?= field($highlights, 'title') ?></span></p>
                            <?php endif; ?>
                            <?php if (field($highlights, 'summary') !== null): ?>
                                <p>요약: <span><?= field($highlights, 'summary') ?></span></p>
                            <?php endif; ?>
                            <?php if (field($highlights, 'content') !== null): ?>
                                <p>본문: <span><?= field($highlights, 'content') ?></span></p>
                            <?php endif; ?>
                        </div>
                    <?php endif; ?>
                </article>
            <?php endforeach; ?>
        </section>
    <?php endif; ?>

    <?php if ($totalElements > 0): ?>
        <nav class="panel">
            <div class="btn-group" style="justify-content: space-between; width: 100%; align-items: center; gap: 12px; flex-wrap: wrap;">
                <div class="btn-group">
                    <?php if ($hasPrevious): ?>
                        <a class="btn btn-ghost" href="<?= e(notesUrl([
                            'keyword' => $keyword,
                            'bookmarkedOnly' => $bookmarkedOnly ? 'true' : 'false',
                            'sort' => $sort,
                            'page' => $page - 1,
                            'size' => $size,
                        ])) ?>">이전</a>
                    <?php else: ?>
                        <span class="subtle">이전</span>
                    <?php endif; ?>
                </div>

                <?php if ($totalPages > 1): ?>
                    <div class="btn-group" style="flex-wrap: wrap;">
                        <?php foreach ($pageNumbers as $pageNumber): ?>
                            <a class="btn <?= $pageNumber === $page ? 'btn-primary' : 'btn-ghost' ?>"
                               href="<?= e(notesUrl([
                                   'keyword' => $keyword,
                                   'bookmarkedOnly' => $bookmarkedOnly ? 'true' : 'false',
                                   'sort' => $sort,
                                   'page' => $pageNumber,
                                   'size' => $size,
                               ])) ?>"><?= e($pageNumber + 1) ?></a>
                        <?php endforeach; ?>
                    </div>
                <?php endif; ?>

                <div class="btn-group" style="align-items: center; gap: 8px;">
                    <span class="subtle"><?= e($currentPageDisplay) ?> / <?= e($totalPages) ?> · 총 <?= e($totalElements) ?>건</span>
                    <?php if ($hasNext): ?>
                        <a class="btn btn-ghost" href="<?= e(notesUrl([
                            'keyword' => $keyword,
                            'bookmarkedOnly' => $bookmarkedOnly ? 'true' : 'false',
                            'sort' => $sort,
                            'page' => $page + 1,
                            'size' => $size,
                        ])) ?>">다음</a>
                    <?php else: ?>
                        <span class="subtle">다음</span>
                    <?php endif; ?>
                </div>
            </div>
        </nav>
    <?php endif; ?>
</main>
</body>
</html>
