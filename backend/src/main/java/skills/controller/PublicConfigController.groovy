package skills.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import skills.HealthChecker
import skills.UIConfigProperties
import skills.profile.EnableCallStackProf

@RestController
@RequestMapping("/public")
@Slf4j
@EnableCallStackProf
class PublicConfigController {

    @Autowired
    HealthChecker healthChecker

    @Autowired
    UIConfigProperties uiConfigProperties

    @RequestMapping(value = "/config", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    Map<String,String> getConfig(){
        return uiConfigProperties.ui
    }

    final private static Map statusRes = [
            status: "OK",
    ]

    @CrossOrigin
    @RequestMapping(value = "/status", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    def status() {
        healthChecker.checkRequiredServices()
        return statusRes
    }
}
