package omsu.imit.controllers;

import omsu.imit.services.InnService;
import omsu.imit.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontController {

    @Autowired
    private InnService innService;

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String welcome() {
        if (userService.getUser() == null) {
            return "setup";
        } else {
            return "redirect:/main";
        }
    }

    @GetMapping("/main")
    public String main(Model model) {
        if (userService.getUser() == null) {
            return welcome();
        }
        model.addAttribute("inn", innService.getInfoAboutAllInn().getBody());
        return "main";
    }
}
