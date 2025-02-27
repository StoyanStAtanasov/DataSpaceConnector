/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Microsoft Corporation - Refactoring
 */

package org.eclipse.dataspaceconnector.spi.contract.offer;

import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A query that returns contract offers for the given parameters.
 */
public class ContractOfferQuery {
    private ClaimToken claimToken;
    private List<Criterion> criteria;
    private long offset;
    private long limit;

    private ContractOfferQuery() {
    }

    public ClaimToken getClaimToken() {
        return claimToken;
    }

    public List<Criterion> getCriteria() {
        return criteria;
    }

    public long getOffset() {
        return offset;
    }

    public long getLimit() {
        return limit;
    }

    public static ContractOfferQuery.Builder builder() {
        return ContractOfferQuery.Builder.newInstance();
    }

    public static final class Builder {
        private ClaimToken claimToken;
        private long offset;
        private long limit;
        private List<Criterion> criteria = new ArrayList<>();

        private Builder() {
        }

        public static Builder newInstance() {
            return new ContractOfferQuery.Builder();
        }

        public Builder claimToken(ClaimToken claimToken) {
            this.claimToken = claimToken;
            return this;
        }

        public Builder criterion(Criterion criterion) {
            this.criteria.add(criterion);
            return this;
        }

        public Builder criteria(Collection<Criterion> criteria) {
            this.criteria.addAll(criteria);
            return this;
        }

        public Builder offset(long offset) {
            this.offset = offset;
            return this;
        }

        public Builder limit(long limit) {
            this.limit = limit;
            return this;
        }

        public ContractOfferQuery build() {
            ContractOfferQuery contractOfferQuery = new ContractOfferQuery();
            contractOfferQuery.claimToken = this.claimToken;
            contractOfferQuery.offset = this.offset;
            contractOfferQuery.limit = this.limit;
            contractOfferQuery.criteria = this.criteria;
            return contractOfferQuery;
        }
    }
}
