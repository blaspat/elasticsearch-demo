package com.demo.elasticsearch.controller;

import com.demo.elasticsearch.bean.StoreBean;
import com.demo.elasticsearch.service.StoreDocService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/store")
@Tag(name = "Store Controller")
public class StoreController {
    private final StoreDocService storeDocService;

    @GetMapping("/search")
    @Operation(summary = "Search store")
    public ResponseEntity<?> search(@RequestParam(required = false) String code,
                                    @RequestParam(required = false) String name) {
        return ResponseEntity.ok(storeDocService.getStoreWithFilter(code, name));
    }

    @PostMapping("")
    @Operation(summary = "Create store")
    public ResponseEntity<?> create(@RequestBody StoreBean bean) {
        storeDocService.createStore(bean);
        return ResponseEntity.ok().build();
    }

    @PutMapping("")
    @Operation(summary = "Update store")
    public ResponseEntity<?> update(@RequestBody StoreBean bean) {
        storeDocService.updateStore(bean);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/update-name")
    @Operation(summary = "Update store name")
    public ResponseEntity<?> updateStoreName(@RequestBody StoreBean bean) {
        storeDocService.updateStoreName(bean.getId(), bean.getName());
        return ResponseEntity.ok().build();
    }
}
