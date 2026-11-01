package com.konstantinos.zito;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.konstantinos.zito.model.dto.OrderResponse;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

class OpenApi30ConfigTest {
    private final OpenApi30Config config = new OpenApi30Config("zito", "test");

    @Test
    void separatesPublicStaffAndRefreshSecurityOnSharedPaths() {
        OpenAPI api = api();
        api.getPaths().addPathItem("/api/v1/orders", new PathItem()
                .get(operation("getOrders")).post(operation("createOrder")));
        api.getPaths().addPathItem("/user/refresh", new PathItem().post(operation("refreshToken")));
        config.restaurantContract().customise(api);

        var orders = api.getPaths().get("/api/v1/orders");
        assertTrue(orders.getPost().getSecurity().isEmpty());
        assertTrue(orders.getGet().getSecurity().get(0).containsKey("bearerAuth"));
        assertTrue(orders.getGet().getResponses().containsKey("403"));
        assertTrue(orders.getPost().getResponses().containsKey("201"));
        assertFalse(orders.getPost().getResponses().containsKey("200"));
        var refresh = api.getPaths().get("/user/refresh").getPost();
        assertTrue(refresh.getSecurity().get(0).containsKey("refreshCookie"));
        assertNotNull(refresh.getResponses().get("200").getContent()
                .get("application/json").getSchema().getProperties().get("userId"));
    }

    @Test
    void onlyCollectionCatalogReadsArePublicAndDeletesHaveNoBody() {
        OpenAPI api = api();
        api.getPaths().addPathItem("/api/v1/menu-items", new PathItem().get(operation("getMenuItems")));
        api.getPaths().addPathItem("/api/v1/menu-items/{id}", new PathItem()
                .get(operation("getMenuItemByName")).delete(operation("DeleteMenuItem")));
        config.restaurantContract().customise(api);
        assertTrue(api.getPaths().get("/api/v1/menu-items").getGet().getSecurity().isEmpty());
        var item = api.getPaths().get("/api/v1/menu-items/{id}");
        assertTrue(item.getGet().getSecurity().get(0).containsKey("bearerAuth"));
        assertTrue(item.getDelete().getResponses().containsKey("204"));
        assertNull(item.getDelete().getResponses().get("204").getContent());
        assertFalse(item.getDelete().getResponses().containsKey("200"));
    }

    @Test
    void consolidatesEquivalentTemplatesAndKeepsCategoryLookupResponses() {
        OpenAPI api = api();
        Operation update = operation("updateCategory").parameters(List.of(
                new Parameter().name("name").in("path").required(true).schema(new StringSchema())));
        api.getPaths().addPathItem("/api/v1/categories/{name}", new PathItem()
                .get(operation("getCategoryByName")).put(update));
        api.getPaths().addPathItem("/api/v1/categories/{id}", new PathItem()
                .get(operation("getCategoryById")).delete(operation("deleteCategory")));
        config.restaurantContract().customise(api);
        assertFalse(api.getPaths().containsKey("/api/v1/categories/{name}"));
        var category = api.getPaths().get("/api/v1/categories/{id}");
        assertNotNull(category.getPut());
        assertNotNull(category.getDelete());
        assertEquals("id", category.getPut().getParameters().get(0).getName());
        assertTrue(category.getGet().getResponses().containsKey("500"));
        assertTrue(category.getGet().getResponses().containsKey("200"));
    }

    @Test
    void documentedMoneyAndDatesMatchApplicationSerialization() throws Exception {
        OpenAPI api = api();
        var response = new ObjectSchema();
        response.addProperty("grossTotal", new NumberSchema());
        response.addProperty("createdAt", new DateTimeSchema());
        api.getComponents().addSchemas("OrderResponse", response);
        config.restaurantContract().customise(api);

        OrderResponse order = new OrderResponse();
        order.setGrossTotal(new BigDecimal("12.40"));
        order.setCreatedAt(new Date(123456789L));
        var mapper = new JacksonConfig().objectMapper();
        var json = mapper.readTree(mapper.writeValueAsString(order));
        assertTrue(json.get("grossTotal").isTextual());
        assertEquals("string", response.getProperties().get("grossTotal").getType());
        assertTrue(json.get("createdAt").isIntegralNumber());
        assertEquals("integer", response.getProperties().get("createdAt").getType());
        assertEquals("int64", response.getProperties().get("createdAt").getFormat());
    }

    private OpenAPI api() {
        return config.customOpenAPI().paths(new Paths());
    }

    private Operation operation(String id) {
        return new Operation().operationId(id).responses(new ApiResponses()
                .addApiResponse("200", new ApiResponse().description("Generated response")));
    }
}
