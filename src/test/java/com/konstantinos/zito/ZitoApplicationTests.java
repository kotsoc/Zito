package com.konstantinos.zito;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.konstantinos.zito.repositories.CategoryRepository;
import com.konstantinos.zito.repositories.ChangedOrderRepository;
import com.konstantinos.zito.repositories.MenuItemRepository;
import com.konstantinos.zito.repositories.OrderRepository;
import com.konstantinos.zito.repositories.TableLayoutRepository;
import com.konstantinos.zito.repositories.TableRepository;
import com.konstantinos.zito.repositories.UserRepository;

@SpringBootTest
@Testcontainers
class ZitoApplicationTests {
	@Mock
	CategoryRepository	categoryRepository;
	@Mock
	ChangedOrderRepository	changedOrderRepository;
	@Mock
	MenuItemRepository	menuItemRepository;
	@Mock
	OrderRepository	orderRepository;
	@Mock
	TableLayoutRepository tyLayoutRepository;
	@Mock
	TableRepository	tableRepository;
	@Mock
	UserRepository userRepository;

	@Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
	
	@Test
	void contextLoads() {
	}

}
