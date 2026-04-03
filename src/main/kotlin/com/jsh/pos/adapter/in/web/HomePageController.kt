package com.jsh.pos.adapter.`in`.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomePageController {

    @GetMapping("/")
    fun home(): String = "redirect:/notes"
}

