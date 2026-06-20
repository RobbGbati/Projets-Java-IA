package com.misterbil.racines.domain.model;

import java.util.List;

/**
 * Une « racine commune » (US7, SPEC §5.3) : deux situations sans rapport
 * textuel qui pointent vers la MÊME croyance via leurs émotions.
 *
 * <p>{@code revelation} est null tant que le LLM n'a pas formulé l'invitation
 * en douceur — la requête graphe seule donne {@code belief} + {@code situations}.</p>
 */
public record CommonRoot(Node belief, List<Node> situations, String revelation) {

    public CommonRoot {
        situations = (situations == null) ? List.of() : List.copyOf(situations);
    }

    public CommonRoot(Node belief, List<Node> situations) {
        this(belief, situations, null);
    }

    public CommonRoot withRevelation(String text) {
        return new CommonRoot(belief, situations, text);
    }
}
