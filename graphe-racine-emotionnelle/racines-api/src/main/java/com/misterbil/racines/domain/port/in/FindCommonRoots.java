package com.misterbil.racines.domain.port.in;

import com.misterbil.racines.domain.model.CommonRoot;

import java.util.List;

/** Cas d'usage : révéler les racines communes (US7). */
public interface FindCommonRoots {
    List<CommonRoot> find();
}
