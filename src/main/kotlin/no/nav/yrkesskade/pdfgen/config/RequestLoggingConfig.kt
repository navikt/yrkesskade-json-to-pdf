package no.nav.yrkesskade.pdfgen.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter

@Configuration
class RequestLoggingConfig {

    @Bean
    fun logFilter() = CommonsRequestLoggingFilter().apply {
        setIncludeQueryString(true)
        setIncludePayload(false)
        setIncludeHeaders(true)
        setIncludeClientInfo(true)
    }
}