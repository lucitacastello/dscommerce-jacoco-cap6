package com.devsuperior.dscommerce.controllers.it;

import com.devsuperior.dscommerce.dto.OrderDTO;
import com.devsuperior.dscommerce.entities.*;
import com.devsuperior.dscommerce.tests.ProductFactory;
import com.devsuperior.dscommerce.tests.TokenUtil;
import com.devsuperior.dscommerce.tests.UserFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Teste de Integração
@SpringBootTest //carrega contexto da app
@AutoConfigureMockMvc
@Transactional
public class OrderControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenUtil tokenUtil; //obter token

    @Autowired
    private ObjectMapper objectMapper; //para usar body da requisição

    private String adminToken, clientToken, invalidToken;
    private String adminUserName, adminPassword, clientUserName, clientPassword;

    private Long existingOrderId, nonExistingOrderId;

    private Order order;
    private OrderDTO orderDTO;

    private Product product;
    private OrderItem orderItem;

    private User user;

    @BeforeEach
    void setUp() throws Exception {

        adminUserName = "alex@gmail.com";
        adminPassword = "123456";

        clientUserName = "maria@gmail.com";
        clientPassword = "123456";

        adminToken = tokenUtil.obtainAccessToken(mockMvc, adminUserName, adminPassword); //obtem token admin
        clientToken = tokenUtil.obtainAccessToken(mockMvc, clientUserName, clientPassword); //obtem tokem client

        invalidToken = adminToken + "xpto"; //simulando password inválida - wrong password

        existingOrderId = 1L; //existe lá no DB
        nonExistingOrderId = 100L;

        user = UserFactory.createClientUser();

        order = new Order(null, Instant.now(), OrderStatus.WAITING_PAYMENT, user, null);

        product = ProductFactory.createProduct();
        orderItem = new OrderItem(order, product, 2, 10.0);
        order.getItems().add(orderItem);
    }

    // Problema 4: Consultar pedido por id

    @Test
    public void findByIdShouldReturnOrderDTOWhenIdExistsAndAdminLogged() throws Exception {
        // 1.	Busca de pedido por id retorna pedido existente quando logado como admin
        ResultActions result = mockMvc
                .perform(get("/orders/{id}", existingOrderId)
                        .header("Authorization", "Bearer " + adminToken) //obter token de admin
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print()); //debugar
        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.id").value(existingOrderId));
        result.andExpect(jsonPath("$.moment").value("2022-07-25T13:00:00Z"));
        result.andExpect(jsonPath("$.status").value("PAID"));
        result.andExpect(jsonPath("$.client").exists());
        result.andExpect(jsonPath("$.client.name").value("Maria Brown"));
        result.andExpect(jsonPath("$.payment").exists());
        result.andExpect(jsonPath("$.items").exists());
        result.andExpect(jsonPath("$.items[1].name").value("Macbook Pro"));
        result.andExpect(jsonPath("$.total").exists());
    }

    @Test
    public void findByIdShouldReturnOrderDTOWhenIdExistsAndClientLogged() throws Exception {
        // 2.	Busca de pedido por id retorna pedido existente quando logado como cliente e o pedido pertence ao usuário
        ResultActions result = mockMvc
                .perform(get("/orders/{id}", existingOrderId)
                        .header("Authorization", "Bearer " + clientToken) //obter token
                        .accept(MediaType.APPLICATION_JSON));
//                .andDo(MockMvcResultHandlers.print()); //debugar
        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.id").value(existingOrderId));
        result.andExpect(jsonPath("$.moment").value("2022-07-25T13:00:00Z"));
        result.andExpect(jsonPath("$.status").value("PAID"));
        result.andExpect(jsonPath("$.client").exists());
        result.andExpect(jsonPath("$.client.name").value("Maria Brown"));
        result.andExpect(jsonPath("$.payment").exists());
        result.andExpect(jsonPath("$.items").exists());
        result.andExpect(jsonPath("$.items[1].name").value("Macbook Pro"));
        result.andExpect(jsonPath("$.total").exists());
    }

    @Test
    public void findByIdShouldReturnForbiddenWhenIdExistsAndClientLoggedAndOrderDoesNotBelongUser403() throws Exception {
        // 3.	Busca de pedido por id retorna 403 quando pedido não pertence ao usuário (com perfil de cliente)
        //pedido ID =2 - pertence ao alex
        Long otherOrderId = 2L;
        ResultActions result = mockMvc
                .perform(get("/orders/{id}", otherOrderId)
                        .header("Authorization", "Bearer " + clientToken) //obter token
                        .accept(MediaType.APPLICATION_JSON));
//                .andDo(MockMvcResultHandlers.print()); //debugar
        result.andExpect(status().isForbidden());
    }

    @Test
    public void findByIdShouldReturnNotFoundWhenIdDoesNotExistsAndAdminLogged404() throws Exception {
        // 4.	Busca de pedido por id retorna 404 para pedido inexistente quando logado como admin

        Long otherOrderId = 2L;
        ResultActions result = mockMvc
                .perform(get("/orders/{id}", nonExistingOrderId)
                        .header("Authorization", "Bearer " + adminToken) //obter token
                        .accept(MediaType.APPLICATION_JSON));
//                .andDo(MockMvcResultHandlers.print()); //debugar
        result.andExpect(status().isNotFound());
    }

    @Test
    public void findByIdShouldReturnNotFoundWhenIdDoesNotExistsAndClientLogged404() throws Exception {
        // 5.	Busca de pedido por id retorna 404 para pedido inexistente quando logado como cliente

        Long otherOrderId = 2L;
        ResultActions result = mockMvc
                .perform(get("/orders/{id}", nonExistingOrderId)
                        .header("Authorization", "Bearer " + clientToken) //obter token
                        .accept(MediaType.APPLICATION_JSON));
//                .andDo(MockMvcResultHandlers.print()); //debugar
        result.andExpect(status().isNotFound());
    }

    @Test
    public void findByIdShouldReturnUnauthorizedtWhenExistingIdAndInvalidToken401() throws Exception {
        // 6.	Busca de pedido por id retorna 401 quando não logado como admin ou cliente

        Long otherOrderId = 2L;
        ResultActions result = mockMvc
                .perform(get("/orders/{id}", existingOrderId)
                        .header("Authorization", "Bearer " + invalidToken) //obter token
                        .accept(MediaType.APPLICATION_JSON));
//                .andDo(MockMvcResultHandlers.print()); //debugar
        result.andExpect(status().isUnauthorized());
    }


}
