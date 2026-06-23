package com.misterbil.racines.adapter.in.web;

import lombok.RequiredArgsConstructor;
import com.misterbil.racines.adapter.in.web.dto.Dtos.CommonRootDto;
import com.misterbil.racines.domain.port.in.FindCommonRoots;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Révélations : racines communes (US7, requête pure graphe). */
@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final FindCommonRoots findCommonRoots;
    @GetMapping("/common-roots")
    public List<CommonRootDto> commonRoots() {
        return findCommonRoots.find().stream().map(WebMapper::commonRoot).toList();
    }
}
