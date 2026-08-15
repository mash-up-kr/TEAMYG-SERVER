package parfait.bootstrap

import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@AutoConfigurationPackage(basePackages = ["parfait"])
@SpringBootApplication(scanBasePackages = ["parfait"])
@EnableScheduling
class ParfaitApplication

fun main(args: Array<String>) {
    runApplication<ParfaitApplication>(*args)
}
