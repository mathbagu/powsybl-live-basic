package cases

network.getLineStream().collect { l ->
    contingency(l.id) {
        equipments l.id
    }
}

network.getGeneratorStream().collect { g ->
    contingency(g.id) {
        equipments g.id
    }
}
