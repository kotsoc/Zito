package com.example.zito.webControllers;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.zito.model.Order;
import com.example.zito.model.Table;
import com.example.zito.model.Waiter;
import com.example.zito.repositories.OrderRepository;
import com.example.zito.repositories.WaiterRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    // private OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    WaiterRepository waiterRepository;

    /*
     * Get all orders for a specific waiter
     */
    @GetMapping("/order/{waiterName}")
    public List<Order> getOrdersByWaiter(@PathVariable("waiterName") String name) {
        Optional<Waiter> waiter = waiterRepository.findById(name);
        return orderRepository.findByWaiter(waiter.get()).stream().sorted(Comparator.comparing(Order::getTable))
                .collect(Collectors.toList());
    }

    /*
     * Get all orders for a table
     */
    // @GetMapping("/{tableId}/order/")
    // public List<Order> getOrdersByTable(@PathVariable("tableId") String tableId)
    // {
    // Table table = new Table();
    // table.setId(tableId);
    // return orderRepository.findByTable(table);
    // }

    /*
     * Create a new order, need to correspond to a valid waiter
     */
    @PostMapping("/order")
    public Order createMyDocument(@Valid @RequestBody Order order) {
        if (order.getWaiter() == null) {
            Waiter waiter = new Waiter("testWaiter");
            waiter.setId("testWaiter");
            waiterRepository.save(waiter);
            order.setWaiter(waiter);
        }
        return orderRepository.save(order);
    }

    // @GetMapping("/my-documents/{id}")
    // public MyDocument getMyDocumentById(@PathVariable("id") String id) {
    // return myRepository.findById(id).orElse(null);
    // }

    // @PutMapping("/my-documents/{id}")
    // public MyDocument updateMyDocument(@PathVariable("id") String id, @Valid
    // @RequestBody MyDocument myDocument) {
    // myDocument.setId(id);
    // return myRepository.save(myDocument);
    // }

    // @DeleteMapping("/my-documents/{id}")
    // public void deleteMyDocumentById(@PathVariable("id") String id) {
    // myRepository.deleteById(id);
    // }

}
