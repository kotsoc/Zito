package com.example.zito.webControllers;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.zito.model.Table;
import com.example.zito.repositories.TableRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/table")
public class TableController {

    private final TableRepository tableRepository;

    public TableController(TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    /*
     * Get all information about a specific waiter
     */
    @GetMapping("/{id}")
    public ResponseEntity<Table> getTable(@PathVariable("id") String id) {
        var res = tableRepository.findById(id);
        if (res.isPresent()) {
            return ResponseEntity.ok(res.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all Tables.
     */
    @GetMapping
    public ResponseEntity<List<Table>> getAllTables() {
        List<Table> Tables = tableRepository.findAll();
        return ResponseEntity.ok(Tables);
    }

    /**
     * Create a new Table.
     */
    @PostMapping
    public ResponseEntity<Table> createTable(@Valid @RequestBody Table Table) {
        Table savedTable = tableRepository.save(Table);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTable);
    }

    /**
     * Update an existing Table.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Table> updateTable(@PathVariable(value = "id") String id,
            @Valid @RequestBody Table TableDetails) {
        Optional<Table> Table = tableRepository.findById(id);
        if (Table.isPresent()) {
            final Table updatedTable = tableRepository.save(TableDetails);
            return ResponseEntity.ok(updatedTable);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTableById(@PathVariable("id") String id) {
        Optional<Table> Table = tableRepository.findById(id);
        if (Table.isPresent()) {
            tableRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}