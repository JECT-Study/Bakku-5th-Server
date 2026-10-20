package com.ject.example.api.controller;

import com.ject.example.api.dto.AddBarRequest;
import com.ject.example.api.dto.CreateFooRequest;
import com.ject.example.api.dto.RenameFooRequest;
import com.ject.example.application.dto.FooResult;
import com.ject.example.application.service.FooService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/examples/foos")
public class FooController {

    private final FooService fooService;

    public FooController(FooService fooService) {
        this.fooService = fooService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FooResult create(@RequestBody CreateFooRequest request) {
        return fooService.create(request.name());
    }

    @GetMapping("/{fooId}")
    public FooResult get(@PathVariable("fooId") Long fooId) {
        return fooService.get(fooId);
    }

    @PatchMapping("/{fooId}/name")
    public FooResult rename(@PathVariable("fooId") Long fooId, @RequestBody RenameFooRequest request) {
        return fooService.rename(fooId, request.name());
    }

    @PostMapping("/{fooId}/bars")
    @ResponseStatus(HttpStatus.CREATED)
    public FooResult addBar(@PathVariable("fooId") Long fooId, @RequestBody AddBarRequest request) {
        return fooService.addBar(fooId, request.content());
    }

    @PostMapping("/{fooId}/archive")
    public FooResult archive(@PathVariable("fooId") Long fooId) {
        return fooService.archive(fooId);
    }
}
