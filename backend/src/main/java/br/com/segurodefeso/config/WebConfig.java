package br.com.segurodefeso.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serve o front-end React (gerado em src/main/resources/static) e devolve o index.html
 * para as rotas do React (/clientes, /prazos...), para que o recarregar da página funcione.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String caminho, Resource local) throws IOException {
                    Resource r = local.createRelative(caminho);
                    if (r.exists() && r.isReadable()) {
                        return r;
                    }
                    if (caminho.startsWith("api/")) {
                        return null;
                    }
                    return new ClassPathResource("/static/index.html");
                }
            });
    }
}
