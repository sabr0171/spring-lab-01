package kz.iitu.spring_lab_01.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// The start page used to be static/index.html, reached through a FORWARD from "/" to "/index.html".
// Spring Security authorises forwards too, and no rule permits "/index.html", so "/" sent anonymous
// users to the login page although "/" itself is permitAll. A controller answers "/" directly.
@Controller
public class HomeController {

    @Value("${app.owner:unknown}")
    private String owner;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("owner", owner);
        return "index";
    }
}
