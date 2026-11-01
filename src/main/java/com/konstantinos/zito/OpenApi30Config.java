package com.konstantinos.zito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApi30Config {

  private final String moduleName;
  private final String apiVersion;

  public OpenApi30Config(
      @Value("${module-name}") String moduleName,
      @Value("${api-version}") String apiVersion) {
    this.moduleName = moduleName;
    this.apiVersion = apiVersion;
  }

  @Bean
  public OpenAPI customOpenAPI() {
    final String securitySchemeName = "bearerAuth";
    final String apiTitle = String.format("%s API", StringUtils.capitalize(moduleName));
    return new OpenAPI()
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .components(
            new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecuritySchemes("refreshCookie", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("refreshToken"))
                .addSecuritySchemes("csrfHeader", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-CSRF-Protection")
                    .description("Send the literal value 1 for refresh and logout.")))
        .info(new Info().title(apiTitle).version(apiVersion).description(
            "Current restaurant ordering API (normal JWT profile). Monetary BigDecimals are JSON strings. "
            + "Payments, refunds and fiscal receipts are not implemented. Tax and percentage discounts are partial. "
            + "See docs/API.md for current limitations. "
            + "The local-dev HTTP Basic profile is not described by this contract."));
  }

  /** Enrich generated schemas/routes without advertising planned endpoints as implemented. */
  @Bean
  public OpenApiCustomizer restaurantContract() {
    return api -> {
      if (api.getPaths() == null) {
        return;
      }
      api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
        boolean get = method == PathItem.HttpMethod.GET;
        boolean publicAccess = Set.of("/user/signin", "/user/signup").contains(path)
            || path.equals("/api/v1/restaurants/{restaurantId}/branding")
            || (get && Set.of("/api/v1/menu-items", "/api/v1/categories", "/api/v1/orders/{orderId}").contains(path))
            || (method == PathItem.HttpMethod.POST && path.equals("/api/v1/orders"));
        boolean refresh = path.equals("/user/refresh");
        operation.setSecurity(publicAccess ? new ArrayList<>() : List.of(
            new SecurityRequirement().addList(refresh ? "refreshCookie" : "bearerAuth")));
        if (refresh || path.equals("/user/signout")) {
          operation.getSecurity().get(0).addList("csrfHeader");
          operation.getResponses().addApiResponse("403", new ApiResponse().description("CSRF header missing or origin rejected"));
        }
        if (!publicAccess) {
          operation.getResponses().addApiResponse("401", new ApiResponse().description("Missing or invalid credentials"));
        }

        String roles = null;
        if (path.startsWith("/api/v1/orders")) {
          if ((path.equals("/api/v1/orders") && get) || path.endsWith("/status")) {
            roles = "ADMIN, WAITER, MANAGER or KITCHEN";
          } else if (path.equals("/api/v1/orders/waiter")) {
            roles = get ? "WAITER" : "ADMIN, MANAGER or WAITER";
          } else if (path.startsWith("/api/v1/orders/waiter/")) {
            roles = "ADMIN or MANAGER";
          } else if (path.endsWith("/cancellation")) {
            roles = "ADMIN or MANAGER";
          } else if (path.contains("/order-point/")) {
            roles = "ADMIN, MANAGER, WAITER or KITCHEN";
          }
        } else if (get && path.startsWith("/api/v1/table")) {
          roles = "ADMIN, MANAGER, WAITER or KITCHEN";
        } else if (path.equals("/user/all") || path.equals("/user/update") || path.equals("/user/{username}")) {
          roles = "ADMIN";
        } else if (!get && (path.startsWith("/api/v1/menu-items")
            || path.startsWith("/api/v1/categories") || path.startsWith("/api/v1/table"))) {
          roles = "ADMIN or MANAGER";
        }
        if (roles != null) {
          operation.setDescription((operation.getDescription() == null ? "" : operation.getDescription() + "\n")
              + "Required roles: " + roles + ".");
          operation.getResponses().addApiResponse("403", new ApiResponse().description("Insufficient role"));
        }

        String id = operation.getOperationId();
        if (id == null) {
          return;
        }
        String note = NOTES.get(id);
        if (note != null) {
          operation.setDescription((operation.getDescription() == null ? "" : operation.getDescription() + "\n") + note);
        }
        String success = CREATED.contains(id) ? "201" : NO_CONTENT.contains(id) ? "204" : "200";
        ApiResponse response = operation.getResponses().remove("200");
        if (response == null) {
          response = new ApiResponse();
        }
        response.setDescription(success.equals("201") ? "Created" : success.equals("204") ? "No content" : "Success");
        if (success.equals("204")) {
          response.setContent(null);
        }
        operation.getResponses().addApiResponse(success, response);
        if (NOT_FOUND.contains(id)) {
          operation.getResponses().addApiResponse("404", new ApiResponse().description("Not found (also used for some empty order lists)"));
        }
        if (BAD_REQUEST.contains(id)) {
          operation.getResponses().addApiResponse("400", new ApiResponse().description("Invalid request"));
        }
        if ("createRestaurantUser".equals(id)) {
          operation.getResponses().addApiResponse("409", new ApiResponse().description("Duplicate user"));
        }
        if ("signIn".equals(id)) {
          operation.getResponses().addApiResponse("401", new ApiResponse().description("Invalid credentials"));
        }
        operation.getResponses().addApiResponse("500", new ApiResponse().description("Unhandled server failure"));
        operation.getResponses().addApiResponse("503", new ApiResponse().description("Dependency unavailable"));
        if (Set.of("updateOrder", "updateOrderStatus", "cancelOrder", "deleteOrder").contains(id)) {
          operation.getResponses().addApiResponse("409", new ApiResponse().description("Stale order version or invalid lifecycle operation"));
          operation.getResponses().addApiResponse("400", new ApiResponse().description("Version and valid request required"));
          operation.getResponses().addApiResponse("404", new ApiResponse().description("Order not found"));
        }
        if (Set.of("signIn", "createRestaurantUser", "refreshToken", "signOut", "createOrder", "getOrder").contains(id)) {
          operation.getResponses().addApiResponse("429", new ApiResponse().description("Per-client-IP request limit exceeded"));
          operation.getResponses().addApiResponse("413", new ApiResponse().description("Payload exceeds configured size limit"));
        }
        operation.getResponses().forEach((status, errorResponse) -> {
          if (status.matches("[45][0-9][0-9]")) {
            errorResponse.setContent(new Content().addMediaType("application/json", new MediaType().schema(
                new Schema<>().type("object").addProperty("code", new Schema<>().type("string"))
                    .addProperty("traceId", new Schema<>().type("string")))));
          }
        });
        if (refresh) {
          response.setContent(new Content().addMediaType("application/json", new MediaType().schema(
              new Schema<>().type("object").addProperty("accessToken", new Schema<>().type("string"))
                  .addProperty("userId", new Schema<>().type("string").format("uuid")))));
          for (String status : List.of("401", "404")) {
            operation.getResponses().get(status).setContent(new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().type("object")
                    .addProperty("code", new Schema<>().type("string"))
                    .addProperty("traceId", new Schema<>().type("string")))));
          }
        }
      }));

      // OpenAPI forbids equivalent templated paths with different parameter names.
      mergePath(api, "/api/v1/menu-items/{name}", "/api/v1/menu-items/{id}", "id");
      mergePath(api, "/api/v1/categories/{name}", "/api/v1/categories/{id}", "id");
      if (api.getComponents() != null && api.getComponents().getSchemas() != null) {
        api.getComponents().getSchemas().values().forEach(this::documentWireTypes);
        describeFields(api, "OrderResponse", List.of("currency", "discount", "tip", "guestEmail", "guestPhone"),
            "Currently not populated by the response mapper; normally null. Not an implemented financial/contact feature.");
        describeFields(api, "OrderItemResponse", List.of("unitPrice", "tax"),
            "Currently sourced from an unset snapshot field; normally null for newly created orders.");
        describeFields(api, "CreateOrderRequest", List.of("currency", "customerNote"),
            "Accepted by the DTO but currently ignored by the order service.");
      }
    };
  }

  private void describeFields(OpenAPI api, String schemaName, List<String> fields, String description) {
    Schema<?> schema = api.getComponents().getSchemas().get(schemaName);
    if (schema != null && schema.getProperties() != null) {
      fields.forEach(name -> {
        Schema<?> field = schema.getProperties().get(name);
        if (field != null) {
          field.setDescription((field.getDescription() == null ? "" : field.getDescription() + " ") + description);
        }
      });
    }
  }

  private void mergePath(OpenAPI api, String source, String target, String parameterName) {
    PathItem old = api.getPaths().remove(source);
    if (old == null) {
      return;
    }
    PathItem destination = api.getPaths().computeIfAbsent(target, key -> new PathItem());
    old.readOperationsMap().forEach((method, operation) -> {
      if (operation.getParameters() != null) {
        operation.getParameters().stream().filter(p -> "path".equals(p.getIn())).forEach(p -> {
          p.setName(parameterName);
          p.setDescription(source.contains("categories") ? "Category name for PUT; integer ID for DELETE. GET is ambiguous." : "Menu item ID (the controller calls this parameter name)");
        });
      }
      destination.operation(method, operation);
    });
  }

  private void documentWireTypes(Schema<?> schema) {
    if ("number".equals(schema.getType()) && schema.getFormat() == null) {
      // All unformatted decimal fields in the current domain are BigDecimal.
      schema.setType("string");
      schema.setDescription((schema.getDescription() == null ? "" : schema.getDescription() + " ")
          + "Decimal serialized as a JSON string by JacksonConfig.");
    }
    if ("date-time".equals(schema.getFormat())) {
      // JacksonConfig uses a plain ObjectMapper with Date timestamps enabled.
      schema.setType("integer");
      schema.setFormat("int64");
      schema.setDescription("Epoch milliseconds (java.util.Date with the current ObjectMapper).");
    }
    if (schema.getProperties() != null) {
      schema.getProperties().values().forEach(this::documentWireTypes);
    }
    if (schema.getItems() != null) {
      documentWireTypes(schema.getItems());
    }
  }

  private static final Set<String> CREATED = Set.of("createRestaurantUser", "PostCategory", "PostMenuItems",
      "createOrder", "createOrderForWaiter", "updateOrder", "createTable", "addTableToLayout");
  private static final Set<String> NO_CONTENT = Set.of("deleteCategory", "DeleteMenuItem", "deleteOrder",
      "deleteTableById", "deleteTableLayoutById");
  private static final Set<String> NOT_FOUND = Set.of("refreshToken", "me", "updateWaiter", "getCategoryByName",
      "getCategoryById", "updateCategory", "getMenuItemByName", "UpdateMenuItem", "getOrder", "getOrdersByWaiter",
      "getOrdersByTable", "getTable", "updateTable", "deleteTableById", "deleteTableLayoutById");
  private static final Set<String> BAD_REQUEST = Set.of("signIn", "createRestaurantUser", "updateWaiter", "PostMenuItems",
      "createOrder", "createOrderForWaiter", "updateOrder", "updateOrderStatus", "createTable", "updateTable", "addTableToLayout");
  private static final Map<String, String> NOTES = Map.ofEntries(
      Map.entry("signIn", "Returns an access JWT and sets the HttpOnly refreshToken cookie, scoped to /user."),
      Map.entry("refreshToken", "Rotates a single-use refresh cookie; reuse revokes further refreshes in its family. Requires X-CSRF-Protection: 1."),
      Map.entry("signOut", "POST logout invalidates the bearer token and refresh family and clears the /user cookie. Requires X-CSRF-Protection: 1."),
      Map.entry("createRestaurantUser", "Creates ROLE_GUEST and returns a safe user DTO."),
      Map.entry("getOrder", "Requires the X-Order-Credential header returned on guest creation; only a hash is stored."),
      Map.entry("createOrder", "Guest discounts must be zero/absent. Currency and customerNote are currently ignored. No idempotency or guest-created notification."),
      Map.entry("createOrderForWaiter", "Staff percentage discounts 0–100. Request cartId, currency and customerNote are not persisted on this path."),
      Map.entry("updateOrder", "Returns 201. ADMIN may edit unpaid NEW/CONFIRMED orders with the current version; no update event is emitted."),
      Map.entry("updateOrderStatus", "Requires current version and the next lifecycle status. KITCHEN may advance to PREPARING/READY. Does not confirm payment."),
      Map.entry("getOrders", "Unpaginated list. Currency, discount, tip and guest contact response fields are not mapped; item unitPrice and tax snapshots are unset."),
      Map.entry("UpdateMenuItem", "Path is a menu item ID; body ID must match. Persists pricing, availability, category, image and modifier fields."),
      Map.entry("updateTable", "Requires an existing path ID and an equal body ID."),
      Map.entry("getBranding", "Unknown restaurant IDs return sample branding with 200, not 404.")
  );
}
