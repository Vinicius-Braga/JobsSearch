package com.jobs.domain;

public enum Plan {
    FREE(0),
    PLUS(500);

    private final long priceCents;

    Plan(long priceCents) {
        this.priceCents = priceCents;
    }

    public long priceCents() {
        return priceCents;
    }

    public String priceDisplay() {
        return "R$" + (priceCents / 100);
    }
}
