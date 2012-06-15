/*
 * Copyright 2010-2011 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.billing.catalog;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.ning.billing.ErrorCode;
import com.ning.billing.catalog.api.ActionPolicy;
import com.ning.billing.catalog.api.BillingAlignment;
import com.ning.billing.catalog.api.BillingPeriod;
import com.ning.billing.catalog.api.CatalogApiException;
import com.ning.billing.catalog.api.Currency;
import com.ning.billing.catalog.api.Listing;
import com.ning.billing.catalog.api.Plan;
import com.ning.billing.catalog.api.PlanAlignmentChange;
import com.ning.billing.catalog.api.PlanAlignmentCreate;
import com.ning.billing.catalog.api.PlanChangeResult;
import com.ning.billing.catalog.api.PlanPhase;
import com.ning.billing.catalog.api.PlanPhaseSpecifier;
import com.ning.billing.catalog.api.PlanSpecifier;
import com.ning.billing.catalog.api.PriceList;
import com.ning.billing.catalog.api.Product;
import com.ning.billing.catalog.api.StaticCatalog;
import com.ning.billing.catalog.rules.PlanRules;
import com.ning.billing.util.config.ValidatingConfig;
import com.ning.billing.util.config.ValidationError;
import com.ning.billing.util.config.ValidationErrors;

@XmlRootElement(name = "catalog")
@XmlAccessorType(XmlAccessType.NONE)
public class StandaloneCatalog extends ValidatingConfig<StandaloneCatalog> implements StaticCatalog {
    @XmlElement(required = true)
    private Date effectiveDate;

    @XmlElement(required = true)
    private String catalogName;

    private URI catalogURI;

    @XmlElementWrapper(name = "currencies", required = true)
    @XmlElement(name = "currency", required = true)
    private Currency[] supportedCurrencies;

    @XmlElementWrapper(name = "products", required = true)
    @XmlElement(name = "product", required = true)
    private DefaultProduct[] products;

    @XmlElement(name = "rules", required = true)
    private PlanRules planRules;

    @XmlElementWrapper(name = "plans", required = true)
    @XmlElement(name = "plan", required = true)
    private DefaultPlan[] plans;

    @XmlElement(name = "priceLists", required = true)
    private DefaultPriceListSet priceLists;

    public StandaloneCatalog() {
    }

    protected StandaloneCatalog(final Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    /* (non-Javadoc)
      * @see com.ning.billing.catalog.ICatalog#getCalalogName()
      */
    @Override
    public String getCatalogName() {
        return catalogName;
    }

    @Override
    public Date getEffectiveDate() {
        return effectiveDate;
    }

    /* (non-Javadoc)
      * @see com.ning.billing.catalog.ICatalog#getProducts()
      */
    @Override
    public DefaultProduct[] getCurrentProducts() {
        return products;
    }


    @Override
    public Currency[] getCurrentSupportedCurrencies() {
        return supportedCurrencies;
    }

    @Override
    public DefaultPlan[] getCurrentPlans() {
        return plans;
    }

    public URI getCatalogURI() {
        return catalogURI;
    }

    public PlanRules getPlanRules() {
        return planRules;
    }

    public DefaultPriceList findCurrentPriceList(final String priceListName) throws CatalogApiException {
        return priceLists.findPriceListFrom(priceListName);
    }

    public DefaultPriceListSet getPriceLists() {
        return this.priceLists;
    }

    /* (non-Javadoc)
      * @see com.ning.billing.catalog.ICatalog#getPlan(java.lang.String, java.lang.String)
      */
    @Override
    public DefaultPlan findCurrentPlan(final String productName, final BillingPeriod period, final String priceListName) throws CatalogApiException {
        if (productName == null) {
            throw new CatalogApiException(ErrorCode.CAT_NULL_PRODUCT_NAME);
        }
        if (priceLists == null) {
            throw new CatalogApiException(ErrorCode.CAT_PRICE_LIST_NOT_FOUND, priceListName);
        }
        final Product product = findCurrentProduct(productName);
        final DefaultPlan result = priceLists.getPlanFrom(priceListName, product, period);
        if (result == null) {
            final String periodString = (period == null) ? "NULL" : period.toString();
            throw new CatalogApiException(ErrorCode.CAT_PLAN_NOT_FOUND, productName, periodString, priceListName);
        }
        return result;
    }

    @Override
    public DefaultPlan findCurrentPlan(final String name) throws CatalogApiException {
        if (name == null || plans == null) {
            throw new CatalogApiException(ErrorCode.CAT_NO_SUCH_PLAN, name);
        }
        for (final DefaultPlan p : plans) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        throw new CatalogApiException(ErrorCode.CAT_NO_SUCH_PLAN, name);
    }

    @Override
    public Product findCurrentProduct(final String name) throws CatalogApiException {
        if (name == null || products == null) {
            throw new CatalogApiException(ErrorCode.CAT_NO_SUCH_PRODUCT, name);
        }
        for (final DefaultProduct p : products) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        throw new CatalogApiException(ErrorCode.CAT_NO_SUCH_PRODUCT, name);
    }

    @Override
    public PlanPhase findCurrentPhase(final String name) throws CatalogApiException {
        if (name == null || plans == null) {
            throw new CatalogApiException(ErrorCode.CAT_NO_SUCH_PHASE, name);
        }

        final String planName = DefaultPlanPhase.planName(name);
        final Plan plan = findCurrentPlan(planName);
        return plan.findPhase(name);
    }

    @Override
    public PriceList findCurrentPricelist(final String name)
            throws CatalogApiException {
        if (name == null || priceLists == null) {
            throw new CatalogApiException(ErrorCode.CAT_PRICE_LIST_NOT_FOUND, name);
        }

        return priceLists.findPriceListFrom(name);
    }


    //////////////////////////////////////////////////////////////////////////////
    //
    // RULES
    //
    //////////////////////////////////////////////////////////////////////////////
    @Override
    public ActionPolicy planChangePolicy(final PlanPhaseSpecifier from, final PlanSpecifier to) throws CatalogApiException {
        return planRules.getPlanChangePolicy(from, to, this);
    }

    @Override
    public PlanAlignmentChange planChangeAlignment(final PlanPhaseSpecifier from, final PlanSpecifier to) throws CatalogApiException {
        return planRules.getPlanChangeAlignment(from, to, this);
    }

    @Override
    public ActionPolicy planCancelPolicy(final PlanPhaseSpecifier planPhase) throws CatalogApiException {
        return planRules.getPlanCancelPolicy(planPhase, this);
    }

    @Override
    public PlanAlignmentCreate planCreateAlignment(final PlanSpecifier specifier) throws CatalogApiException {
        return planRules.getPlanCreateAlignment(specifier, this);
    }

    @Override
    public BillingAlignment billingAlignment(final PlanPhaseSpecifier planPhase) throws CatalogApiException {
        return planRules.getBillingAlignment(planPhase, this);
    }

    @Override
    public PlanChangeResult planChange(final PlanPhaseSpecifier from, final PlanSpecifier to)
            throws CatalogApiException {
        return planRules.planChange(from, to, this);
    }

    @Override
    public ValidationErrors validate(final StandaloneCatalog catalog, final ValidationErrors errors) {
        validate(catalog, errors, products);
        validate(catalog, errors, plans);
        priceLists.validate(catalog, errors);
        planRules.validate(catalog, errors);
        return errors;
    }

    private Collection<? extends ValidationError> validate(final StandaloneCatalog catalog,
                                                           final ValidationErrors errors, final ValidatingConfig<StandaloneCatalog>[] configs) {
        for (final ValidatingConfig<StandaloneCatalog> config : configs) {
            config.validate(catalog, errors);
        }
        return errors;
    }


    @Override
    public void initialize(final StandaloneCatalog catalog, final URI sourceURI) {
        catalogURI = sourceURI;
        super.initialize(catalog, sourceURI);
        planRules.initialize(catalog, sourceURI);
        priceLists.initialize(catalog, sourceURI);
        for (final DefaultProduct p : products) {
            p.initialize(catalog, sourceURI);
        }
        for (final DefaultPlan p : plans) {
            p.initialize(catalog, sourceURI);
        }

    }


    protected StandaloneCatalog setProducts(final DefaultProduct[] products) {
        this.products = products;
        return this;
    }

    protected StandaloneCatalog setSupportedCurrencies(final Currency[] supportedCurrencies) {
        this.supportedCurrencies = supportedCurrencies;
        return this;
    }

    protected StandaloneCatalog setPlanChangeRules(final PlanRules planChangeRules) {
        this.planRules = planChangeRules;
        return this;
    }

    protected StandaloneCatalog setPlans(final DefaultPlan[] plans) {
        this.plans = plans;
        return this;
    }

    protected StandaloneCatalog setEffectiveDate(final Date effectiveDate) {
        this.effectiveDate = effectiveDate;
        return this;
    }

    protected StandaloneCatalog setPlanRules(final PlanRules planRules) {
        this.planRules = planRules;
        return this;
    }

    protected StandaloneCatalog setPriceLists(final DefaultPriceListSet priceLists) {
        this.priceLists = priceLists;
        return this;
    }

    @Override
    public boolean canCreatePlan(final PlanSpecifier specifier) throws CatalogApiException {
        final Product product = findCurrentProduct(specifier.getProductName());
        final Plan plan = findCurrentPlan(specifier.getProductName(), specifier.getBillingPeriod(), specifier.getPriceListName());
        final DefaultPriceList priceList = findCurrentPriceList(specifier.getPriceListName());

        return (!product.isRetired()) &&
                (!plan.isRetired()) &&
                (!priceList.isRetired());
    }

    @Override
    public List<Listing> getAvailableAddonListings(final String baseProductName) {
        final List<Listing> availAddons = new ArrayList<Listing>();

        try {
            final Product product = findCurrentProduct(baseProductName);
            if (product != null) {
                for (final Product availAddon : product.getAvailable()) {
                    for (final Plan plan : getCurrentPlans()) {
                        if (plan.getProduct().equals(availAddon)) {
                            for (final PriceList priceList : getPriceLists().getAllPriceLists()) {
                                if (priceList.findPlan(availAddon, plan.getBillingPeriod()) != null &&
                                        priceList.findPlan(product, plan.getBillingPeriod()).equals(plan)) {
                                    availAddons.add(new DefaultListing(plan, priceList));
                                }
                            }
                        }
                    }
                }
            }
        } catch (CatalogApiException e) {
            // No such product - just return an empty list
        }

        return availAddons;
    }

}
