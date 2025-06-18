package org.reminstant.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Reservation Service API"))
        .components(new Components()
            .addSecuritySchemes(
                "BearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
  }

//  @Bean
//  public GroupedOpenApi.Builder groupedOpenApiBuilder() {
//    return GroupedOpenApi.builder()
//        .group("ReservationService")
//        .addOpenApiCustomizer(openApi -> {
//          List<String> order = List.of("Авторизация", "Бронирование", "Управление");
//          List<Tag> tags = new ArrayList<>();
//
//          openApi.getTags().forEach(tg -> {
//            if (tg.getName().equalsIgnoreCase("Управление")) reordered[0] = tg;
//            if (tg.getName().equalsIgnoreCase("User Access")) reordered[1] = tg;
//            if (tg.getName().equalsIgnoreCase("Tagging")) reordered[2] = tg;
//            // and so on
//          });
//          openApi.tags(Arrays.asList(reordered));
//        })
//        .pathsToMatch("/api/**");
//  }
}
