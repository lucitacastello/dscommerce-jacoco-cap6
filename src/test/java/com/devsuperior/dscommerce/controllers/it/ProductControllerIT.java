package com.devsuperior.dscommerce.controllers.it;

import com.devsuperior.dscommerce.dto.ProductDTO;
import com.devsuperior.dscommerce.entities.Category;
import com.devsuperior.dscommerce.entities.Product;
import com.devsuperior.dscommerce.tests.TokenUtil;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Teste de Integração
@SpringBootTest //carrega contexto da app
@AutoConfigureMockMvc
@Transactional
public class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenUtil tokenUtil; //obter token

    @Autowired
    private ObjectMapper objectMapper; //para usar body da requisição

    private String productName;

    private String adminToken, clientToken, invalidToken;
    private String adminUserName, adminPassword, clientUserName, clientPassword;

    //para usar no insert
    private Product product;
    private ProductDTO productDTO;

    private Long existingProductId, nonExistingProductId, dependentProductId;

    @BeforeEach
    void setUp() throws Exception {

        adminUserName = "alex@gmail.com";
        adminPassword = "123456";

        clientUserName = "maria@gmail.com";
        clientPassword = "123456";

        productName = "Macbook";

        adminToken = tokenUtil.obtainAccessToken(mockMvc, adminUserName, adminPassword); //obtem token admin
        clientToken = tokenUtil.obtainAccessToken(mockMvc, clientUserName, clientPassword); //obtem tokem client

        invalidToken = adminToken + "xpto"; //simulando password inválida - wrong password

        // inicializando product e productDTO
        Category category = new Category(2L, "Eletro");
        product = new Product(null, "Console PlayStation 5", "Lorem ipsum, dolor sit amet consectetur adipisicing elit.", 3999.90, "https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/1-big.jpg");
        //associando categoria ao produto
        product.getCategories().add(category);

        productDTO = new ProductDTO(product);

        existingProductId = 2L;
        nonExistingProductId = 100L;
        dependentProductId = 3L;
    }

    @Test
    public void findAllShouldRetunrPageWhenParamIsNotEmpty() throws Exception {
        ResultActions result = mockMvc
                .perform(get("/products?name={productName}", productName)
                        .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.content[0].id").value(3L));
        result.andExpect(jsonPath("$.content[0].name").value("Macbook Pro"));
        result.andExpect(jsonPath("$.content[0].price").value(1250.0));
        result.andExpect(jsonPath("$.content[0].imgUrl").value("https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/3-big.jpg"));
    }

    @Test
    public void findAllShouldRetunrPageWhenParamIsEmpty() throws Exception {
        ResultActions result = mockMvc
                .perform(get("/products")
                        .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.content[0].id").value(1L));
        result.andExpect(jsonPath("$.content[0].name").value("The Lord of the Rings"));
        result.andExpect(jsonPath("$.content[0].price").value(90.5));
        result.andExpect(jsonPath("$.content[0].imgUrl").value("https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/1-big.jpg"));
    }

    //    Problema 2: Inserir produto
    @Test
    public void insertShouldReturnProductDTOCreatedWhenAdminLogged() throws Exception {
//      1.	Inserção de produto insere produto com dados válidos quando logado como admin
        String jsonBody = objectMapper.writeValueAsString(productDTO);
        ResultActions result = mockMvc
                .perform(post("/products")
                        .header("Authorization", "Bearer " + adminToken) //obter token de admin
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print()); //debugar

        result.andExpect(status().isCreated());
        result.andExpect(jsonPath("$.id").value(26L)); //tem 25 na lista
        result.andExpect(jsonPath("$.name").value("Console PlayStation 5"));
        result.andExpect(jsonPath("$.description").value("Lorem ipsum, dolor sit amet consectetur adipisicing elit."));
        result.andExpect(jsonPath("$.price").value(3999.90));
        result.andExpect(jsonPath("$.imgUrl").value("https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/1-big.jpg"));
        result.andExpect(jsonPath("$.categories[0].id").value(2L));
    }


    @Test
    public void insertShouldReturnUnporcessableEntityWhenAdminLoggedAndInvalidName422() throws Exception {
        // 2.Inserção de produto retorna 422 e mensagens customizadas com dados inválidos quando logado como admin e campo name for inválido
        product.setName("ab");
        productDTO = new ProductDTO(product);

        String jsonBody = objectMapper.writeValueAsString(productDTO);
        ResultActions result = mockMvc
                .perform(post("/products")
                        .header("Authorization", "Bearer " + adminToken) //obter token de admin
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print()); //debugar
        result.andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void insertShouldReturnUnporcessableEntityWhenAdminLoggedAndInvalidDescription422() throws Exception {
        // 3.	Inserção de produto retorna 422 e mensagens customizadas com dados inválidos quando logado como admin e campo description for inválido
        product.setDescription("ab");
        productDTO = new ProductDTO(product);

        String jsonBody = objectMapper.writeValueAsString(productDTO);
        ResultActions result = mockMvc
                .perform(post("/products")
                        .header("Authorization", "Bearer " + adminToken) //obter token de admin
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print()); //debugar
        result.andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void insertShouldReturnUnporcessableEntityWhenAdminLoggedAndPriceIsNegative422() throws Exception {
        // 4.	Inserção de produto retorna 422 e mensagens customizadas com dados inválidos quando logado como admin e campo price for negativo
        product.setPrice(-50.0);
        productDTO = new ProductDTO(product);

        String jsonBody = objectMapper.writeValueAsString(productDTO);
        ResultActions result = mockMvc
                .perform(post("/products")
                        .header("Authorization", "Bearer " + adminToken) //obter token de admin
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print()); //debugar
        result.andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void insertShouldReturnUnporcessableEntityWhenAdminLoggedAndPriceIsZero422() throws Exception {
        // 4.	Inserção de produto retorna 422 e mensagens customizadas com dados inválidos quando logado como admin e campo price for negativo
        product.setPrice(0.0);
        productDTO = new ProductDTO(product);

        String jsonBody = objectMapper.writeValueAsString(productDTO);
        ResultActions result = mockMvc
                .perform(post("/products")
                        .header("Authorization", "Bearer " + adminToken) //obter token de admin
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print()); //debugar
        result.andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void insertShouldReturnUnporcessableEntityWhenAdminLoggedAndProductHasNotCategory422() throws Exception {
        // 6.	Inserção de produto retorna 422 e mensagens customizadas com dados inválidos quando logado como admin e não tiver categoria associada
        product.getCategories().clear();
        productDTO = new ProductDTO(product);

        String jsonBody = objectMapper.writeValueAsString(productDTO);
        ResultActions result = mockMvc
                .perform(post("/products")
                        .header("Authorization", "Bearer " + adminToken) //obter token de admin
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print()); //debugar
        result.andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void insertShouldReturnForbiddenWhenClientLogged403() throws Exception {
        // 7.	Inserção de produto retorna 403 quando logado como cliente

        String jsonBody = objectMapper.writeValueAsString(productDTO);
        ResultActions result = mockMvc
                .perform(post("/products")
                        .header("Authorization", "Bearer " + clientToken) //obter token de client
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print()); //debugar
        result.andExpect(status().isForbidden());
    }

    @Test
    public void insertShouldReturnUnauthorizedWhenInvalidToken401() throws Exception {
        // 8.	Inserção de produto retorna 401 quando não logado como admin ou cliente

        String jsonBody = objectMapper.writeValueAsString(productDTO);
        ResultActions result = mockMvc
                .perform(post("/products")
                        .header("Authorization", "Bearer " + invalidToken) //obter token de client
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print()); //debugar
        result.andExpect(status().isUnauthorized());
    }

    //     Problema 3: Deletar produto
    @Test
    public void deleteShouldReturnNoContentWhenIdExistsAndAdminLogged204() throws Exception {
        // 1.	Deleção de produto deleta produto existente quando logado como admin

        ResultActions result = mockMvc
                .perform(delete("/products/{id}", existingProductId)
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print()); //debuggar
        result.andExpect(status().isNoContent());
    }

    @Test
    public void deleteShouldReturnNotFoundWhenIdDoesNotExistsAndAdminLogged404() throws Exception {
        // 2.	Deleção de produto retorna 404 para produto inexistente quando logado como admin

        ResultActions result = mockMvc
                .perform(delete("/products/{id}", nonExistingProductId)
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print()); //debuggar
        result.andExpect(status().isNotFound());
    }

    @Test
    @Transactional(propagation = Propagation.SUPPORTS) //para não dar erro pq é dependente
    public void deleteShouldReturnBadRequestWhenDependentIdAndAdminLogged400() throws Exception {
        // 3.	Deleção de produto retorna 400 para produto dependente quando logado como admin

        ResultActions result = mockMvc
                .perform(delete("/products/{id}", dependentProductId)
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON));
             //   .andDo(MockMvcResultHandlers.print()); //debuggar
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void deleteShouldReturnForbiddenWhenClientLogged403() throws Exception {
        // 4.	Deleção de produto retorna 403 quando logado como cliente

        ResultActions result = mockMvc
                .perform(delete("/products/{id}", existingProductId)
                        .header("Authorization", "Bearer " + clientToken)
                        .accept(MediaType.APPLICATION_JSON));
        //   .andDo(MockMvcResultHandlers.print()); //debuggar
        result.andExpect(status().isForbidden());
    }

    @Test
    public void deleteShouldReturnUnauthorizedtWhenExistingIdAndInvalidToken401() throws Exception {
        // 5.	Deleção de produto retorna 401 quando não logado como admin ou cliente

        ResultActions result = mockMvc
                .perform(delete("/products/{id}", existingProductId)
                        .header("Authorization", "Bearer " + invalidToken)
                        .accept(MediaType.APPLICATION_JSON));
        //   .andDo(MockMvcResultHandlers.print()); //debuggar
        result.andExpect(status().isUnauthorized());
    }
}
