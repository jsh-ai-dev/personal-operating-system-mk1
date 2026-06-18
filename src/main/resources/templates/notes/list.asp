<%
' Classic ASP version of notes/list.html for comparison only.
'
' This file is not wired into the Spring Boot app. Thymeleaf resolves
' notes/list to notes/list.html, so this .asp file should not affect runtime.
'
' Expected Classic ASP view variables:
' notes, keyword, bookmarkedOnly, sort, page, size, totalElements,
' totalPages, hasPrevious, hasNext, currentPageDisplay, pageNumbers,
' highlightsById, tagsDisplayById, createdAtDisplayById, updatedAtDisplayById,
' message, csrfParameterName, csrfToken
'
' This sample assumes notes is an array of Scripting.Dictionary-like objects,
' and the *ById variables are Scripting.Dictionary-like maps.

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

Function MapValue(source, key, fallback)
    MapValue = Field(source, CStr(key), fallback)
End Function

Function MapObjectValue(source, key)
    On Error Resume Next
    Set MapObjectValue = Nothing

    If IsObject(source) Then
        If source.Exists(CStr(key)) Then
            If IsObject(source(CStr(key))) Then
                Set MapObjectValue = source(CStr(key))
            End If
        End If
    End If

    If Err.Number <> 0 Then
        Err.Clear
        Set MapObjectValue = Nothing
    End If
    On Error GoTo 0
End Function

Function HasArrayItems(items)
    On Error Resume Next
    HasArrayItems = False

    If IsArray(items) Then
        HasArrayItems = (UBound(items) >= LBound(items))
    End If

    If Err.Number <> 0 Then
        Err.Clear
        HasArrayItems = False
    End If
    On Error GoTo 0
End Function

Function IsPresent(value)
    IsPresent = Not (IsNull(value) Or IsEmpty(value) Or CStr(value) = "")
End Function

Function NotesUrl(keywordValue, bookmarkedOnlyValue, sortValue, pageValue, sizeValue)
    NotesUrl = "/notes?keyword=" & Server.URLEncode(CStr(keywordValue)) & _
        "&bookmarkedOnly=" & Server.URLEncode(CStr(LCase(bookmarkedOnlyValue))) & _
        "&sort=" & Server.URLEncode(CStr(sortValue)) & _
        "&page=" & Server.URLEncode(CStr(pageValue)) & _
        "&size=" & Server.URLEncode(CStr(sizeValue))
End Function

If IsEmpty(keyword) Then keyword = ""
If IsEmpty(bookmarkedOnly) Then bookmarkedOnly = False
If IsEmpty(sort) Or sort = "" Then sort = "created"
If IsEmpty(page) Then page = 0
If IsEmpty(size) Then size = 20
If IsEmpty(totalElements) Then totalElements = 0
If IsEmpty(totalPages) Then totalPages = 0
If IsEmpty(hasPrevious) Then hasPrevious = False
If IsEmpty(hasNext) Then hasNext = False
If IsEmpty(currentPageDisplay) Then currentPageDisplay = page + 1
If IsEmpty(csrfParameterName) Or csrfParameterName = "" Then csrfParameterName = "_csrf"
If IsEmpty(csrfToken) Then csrfToken = ""
%>
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
                <input type="hidden" name="<%= H(csrfParameterName) %>" value="<%= H(csrfToken) %>">
                <label class="btn btn-ghost upload-label" title=".txt 또는 .pdf 파일을 노트로 가져옵니다">
                    파일 가져오기
                    <input type="file" name="file" accept=".txt,.pdf,text/plain,application/pdf" class="upload-input" onchange="this.form.submit()">
                </label>
            </form>
        </div>
    </section>

    <% If IsPresent(message) Then %>
        <div class="message"><%= H(message) %></div>
    <% End If %>

    <form method="get" action="/notes" class="panel search-row">
        <input class="input" type="text" name="keyword" placeholder="제목/본문 검색" value="<%= H(keyword) %>">
        <input type="hidden" name="page" value="0">
        <input type="hidden" name="size" value="<%= H(size) %>">
        <select class="input input-inline" name="sort">
            <option value="created" <% If sort = "created" Then Response.Write "selected" %>>작성일순</option>
            <option value="recent" <% If sort = "recent" Then Response.Write "selected" %>>수정일순</option>
            <option value="title" <% If sort = "title" Then Response.Write "selected" %>>제목순</option>
            <option value="relevance" <% If sort = "relevance" Then Response.Write "selected" %>>관련도순</option>
        </select>
        <label class="checkbox-inline">
            <input type="checkbox" name="bookmarkedOnly" value="true" <% If bookmarkedOnly Then Response.Write "checked" %>>
            북마크만 보기
        </label>
        <button class="btn" type="submit">검색</button>
        <a class="btn btn-ghost" href="/notes">초기화</a>
    </form>

    <% If Not HasArrayItems(notes) Then %>
        <div class="panel empty">
            아직 노트가 없습니다. 첫 노트를 작성해보세요.
        </div>
    <% End If %>

    <% If HasArrayItems(notes) Then %>
        <section class="note-grid">
            <%
            Dim note, id, title, hasStoredFile, originalFileName, isTxtUpload, highlights
            For Each note In notes
                id = CStr(Field(note, "id", ""))
                title = Field(note, "title", "")
                hasStoredFile = CBool(Field(note, "hasStoredFile", False))
                originalFileName = Field(note, "originalFileName", "")
                isTxtUpload = IsPresent(originalFileName) And Not hasStoredFile And LCase(Right(CStr(originalFileName), 4)) = ".txt"
                Set highlights = MapObjectValue(highlightsById, id)
            %>
                <article class="note-card">
                    <div class="note-head">
                        <h2><a href="/notes/<%= H(id) %>"><%= H(title) %></a></h2>
                        <div class="note-card-badges">
                            <% If hasStoredFile Then %>
                                <span class="file-badge" title="원본 파일 저장됨">PDF</span>
                            <% End If %>
                            <% If isTxtUpload Then %>
                                <span class="file-badge file-badge-txt" title="텍스트 파일 업로드 노트">TXT</span>
                            <% End If %>
                            <% If IsPresent(Field(note, "aiSummary", "")) Then %>
                                <span class="summary-badge" title="AI 요약 저장됨">요약</span>
                            <% End If %>
                            <% If CBool(Field(note, "bookmarked", False)) Then %>
                                <span class="bookmark" title="북마크됨">*</span>
                            <% End If %>
                        </div>
                    </div>

                    <% If Not hasStoredFile Then %>
                        <p class="note-content"><%= H(Field(note, "content", "")) %></p>
                    <% Else %>
                        <p class="note-content">PDF 파일 노트입니다. 상세 화면에서 다운로드할 수 있습니다.</p>
                    <% End If %>

                    <div class="meta">
                        <span class="badge"><%= H(Field(note, "visibility", "PRIVATE")) %></span>
                        <% If IsPresent(originalFileName) Then %>
                            <span>파일: <strong><%= H(originalFileName) %></strong></span>
                        <% End If %>
                        <span><%= H(MapValue(tagsDisplayById, id, "-")) %></span>
                        <span>생성: <strong><%= H(MapValue(createdAtDisplayById, id, "-")) %></strong></span>
                        <span>수정: <strong><%= H(MapValue(updatedAtDisplayById, id, "-")) %></strong></span>
                    </div>

                    <% If Not highlights Is Nothing Then %>
                        <div class="search-snippet">
                            <% If IsPresent(Field(highlights, "title", "")) Then %>
                                <p>제목: <span><%= Field(highlights, "title", "") %></span></p>
                            <% End If %>
                            <% If IsPresent(Field(highlights, "summary", "")) Then %>
                                <p>요약: <span><%= Field(highlights, "summary", "") %></span></p>
                            <% End If %>
                            <% If IsPresent(Field(highlights, "content", "")) Then %>
                                <p>본문: <span><%= Field(highlights, "content", "") %></span></p>
                            <% End If %>
                        </div>
                    <% End If %>
                </article>
            <% Next %>
        </section>
    <% End If %>

    <% If totalElements > 0 Then %>
        <nav class="panel">
            <div class="btn-group" style="justify-content: space-between; width: 100%; align-items: center; gap: 12px; flex-wrap: wrap;">
                <div class="btn-group">
                    <% If hasPrevious Then %>
                        <a class="btn btn-ghost" href="<%= H(NotesUrl(keyword, bookmarkedOnly, sort, page - 1, size)) %>">이전</a>
                    <% Else %>
                        <span class="subtle">이전</span>
                    <% End If %>
                </div>

                <% If totalPages > 1 And HasArrayItems(pageNumbers) Then %>
                    <div class="btn-group" style="flex-wrap: wrap;">
                        <%
                        Dim pageNumber, pageClass
                        For Each pageNumber In pageNumbers
                            If pageNumber = page Then
                                pageClass = "btn-primary"
                            Else
                                pageClass = "btn-ghost"
                            End If
                        %>
                            <a class="btn <%= H(pageClass) %>"
                               href="<%= H(NotesUrl(keyword, bookmarkedOnly, sort, pageNumber, size)) %>"><%= H(pageNumber + 1) %></a>
                        <% Next %>
                    </div>
                <% End If %>

                <div class="btn-group" style="align-items: center; gap: 8px;">
                    <span class="subtle"><%= H(currentPageDisplay) %> / <%= H(totalPages) %> · 총 <%= H(totalElements) %>건</span>
                    <% If hasNext Then %>
                        <a class="btn btn-ghost" href="<%= H(NotesUrl(keyword, bookmarkedOnly, sort, page + 1, size)) %>">다음</a>
                    <% Else %>
                        <span class="subtle">다음</span>
                    <% End If %>
                </div>
            </div>
        </nav>
    <% End If %>
</main>
</body>
</html>
