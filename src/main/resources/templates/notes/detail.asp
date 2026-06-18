<%
' Classic ASP version of notes/detail.html for comparison only.
'
' This file is not wired into the Spring Boot app. Thymeleaf resolves
' notes/detail to notes/detail.html, so this .asp file should not affect runtime.
'
' Expected Classic ASP view variables:
' note, tagsDisplay, message, selectedSummaryModelTier, generatedSummary,
' csrfParameterName, csrfToken
'
' This sample assumes note is a Scripting.Dictionary-like object.

Function H(value)
    If IsNull(value) Or IsEmpty(value) Then
        H = ""
    Else
        H = Server.HTMLEncode(CStr(value))
    End If
End Function

Function Field(source, name, fallback)
    On Error Resume Next
    Field = fallback

    If IsObject(source) Then
        If source.Exists(name) Then
            Field = source(name)
        End If
    End If

    If Err.Number <> 0 Then
        Err.Clear
        Field = fallback
    End If
    On Error GoTo 0
End Function

Function IsPresent(value)
    IsPresent = Not (IsNull(value) Or IsEmpty(value) Or CStr(value) = "")
End Function

If IsEmpty(tagsDisplay) Or tagsDisplay = "" Then tagsDisplay = "-"
If IsEmpty(selectedSummaryModelTier) Or selectedSummaryModelTier = "" Then selectedSummaryModelTier = "gpt-5-nano"
If IsEmpty(csrfParameterName) Or csrfParameterName = "" Then csrfParameterName = "_csrf"
If IsEmpty(csrfToken) Then csrfToken = ""

Dim noteId, hasStoredFile, originalFileName, fileContentType, bookmarked, aiSummary
noteId = CStr(Field(note, "id", ""))
hasStoredFile = CBool(Field(note, "hasStoredFile", False))
originalFileName = Field(note, "originalFileName", "")
fileContentType = Field(note, "fileContentType", "")
bookmarked = CBool(Field(note, "bookmarked", False))
aiSummary = Field(note, "aiSummary", "")
%>
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
        <h1><%= H(Field(note, "title", "제목")) %></h1>

        <% If IsPresent(message) Then %>
            <div class="message"><%= H(message) %></div>
        <% End If %>

        <div class="meta">
            <span class="badge"><%= H(Field(note, "visibility", "PRIVATE")) %></span>
            <span>태그: <strong><%= H(tagsDisplay) %></strong></span>
            <span>북마크: <strong><% If bookmarked Then Response.Write "ON" Else Response.Write "OFF" %></strong></span>
            <% If IsPresent(originalFileName) Then %>
                <span>원본 파일: <strong><%= H(originalFileName) %></strong></span>
            <% End If %>
            <% If hasStoredFile And IsPresent(fileContentType) Then %>
                <span>형식: <strong><%= H(fileContentType) %></strong></span>
            <% End If %>
        </div>

        <% If Not hasStoredFile Then %>
            <pre class="note-pre"><%= H(Field(note, "content", "")) %></pre>
        <% Else %>
            <div class="panel">
                이 파일 노트는 브라우저 내 미리보기를 지원하지 않습니다. 아래 다운로드 버튼으로 원본 파일을 내려받아 확인해주세요.
            </div>
        <% End If %>

        <div class="actions">
            <% If Not hasStoredFile Then %>
                <a class="btn" href="/notes/<%= H(noteId) %>/edit">수정</a>
            <% End If %>

            <% If IsPresent(originalFileName) Then %>
                <a class="btn btn-ghost" href="/notes/<%= H(noteId) %>/download">다운로드</a>
            <% End If %>

            <% If Not bookmarked Then %>
                <form action="/notes/<%= H(noteId) %>/bookmark" method="post">
                    <input type="hidden" name="<%= H(csrfParameterName) %>" value="<%= H(csrfToken) %>">
                    <button class="btn" type="submit">북마크</button>
                </form>
            <% End If %>

            <% If bookmarked Then %>
                <form action="/notes/<%= H(noteId) %>/unbookmark" method="post">
                    <input type="hidden" name="<%= H(csrfParameterName) %>" value="<%= H(csrfToken) %>">
                    <button class="btn btn-ghost" type="submit">북마크 해제</button>
                </form>
            <% End If %>

            <form action="/notes/<%= H(noteId) %>/delete" method="post" onsubmit="return confirm('정말 삭제할까요?');">
                <input type="hidden" name="<%= H(csrfParameterName) %>" value="<%= H(csrfToken) %>">
                <button class="btn btn-danger" type="submit">삭제</button>
            </form>
        </div>

        <section class="panel detail-card">
            <h2>AI 요약</h2>

            <% If IsPresent(aiSummary) Then %>
                <div class="summary-box"><%= H(aiSummary) %></div>
            <% Else %>
                <p class="subtle">아직 저장된 요약이 없습니다.</p>
            <% End If %>

            <form class="actions" action="/notes/<%= H(noteId) %>/summary/generate" method="post">
                <input type="hidden" name="<%= H(csrfParameterName) %>" value="<%= H(csrfToken) %>">
                <select class="input input-inline" name="modelTier">
                    <option value="gpt-5-nano" <% If selectedSummaryModelTier = "gpt-5-nano" Then Response.Write "selected" %>>gpt-5-nano ($0.05 / $0.4)</option>
                    <option value="gpt-5-mini" <% If selectedSummaryModelTier = "gpt-5-mini" Then Response.Write "selected" %>>gpt-5-mini ($0.25 / $2)</option>
                    <option value="gpt-5" disabled>gpt-5 ($1.25 / $10)</option>
                </select>
                <button class="btn btn-primary" type="submit">AI 요약 생성</button>
            </form>

            <% If IsPresent(generatedSummary) Then %>
                <form action="/notes/<%= H(noteId) %>/summary/save" method="post" class="page-stack">
                    <input type="hidden" name="<%= H(csrfParameterName) %>" value="<%= H(csrfToken) %>">
                    <label for="summary">생성된 요약 (저장 전)</label>
                    <textarea id="summary" name="summary" class="input" rows="12"><%= H(generatedSummary) %></textarea>
                    <div class="actions">
                        <button class="btn btn-primary" type="submit">요약 저장</button>
                    </div>
                </form>
            <% End If %>
        </section>
    </article>
</main>
</body>
</html>
