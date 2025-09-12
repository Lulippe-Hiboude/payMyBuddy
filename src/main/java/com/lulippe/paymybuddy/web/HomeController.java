package com.lulippe.paymybuddy.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    @GetMapping("/transfer")
    public String showTransferPage(HttpServletRequest request, Model model) {
        log.info("showTransferPage");
        model.addAttribute("currentPath", request.getRequestURI());
        return "home/transfer";
    }
}
