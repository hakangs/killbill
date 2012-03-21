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

package com.ning.billing.analytics;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.ning.billing.account.api.AccountApiException;
import com.ning.billing.account.api.AccountChangeNotification;
import com.ning.billing.account.api.AccountCreationNotification;
import com.ning.billing.entitlement.api.user.SubscriptionTransition;
import com.ning.billing.invoice.api.InvoiceCreationNotification;
import com.ning.billing.payment.api.PaymentError;
import com.ning.billing.payment.api.PaymentInfo;

public class AnalyticsListener {
    private final BusinessSubscriptionTransitionRecorder bstRecorder;
    private final BusinessAccountRecorder bacRecorder;

    @Inject
    public AnalyticsListener(final BusinessSubscriptionTransitionRecorder bstRecorder, final BusinessAccountRecorder bacRecorder) {
        this.bstRecorder = bstRecorder;
        this.bacRecorder = bacRecorder;
    }

    @Subscribe
    public void handleSubscriptionTransitionChange(final SubscriptionTransition event) throws AccountApiException {
        switch (event.getTransitionType()) {
            // A susbcription enters either through migration or as newly created subscription
            case MIGRATE_ENTITLEMENT:
            case CREATE:
                bstRecorder.subscriptionCreated(event);
                break;
            case RE_CREATE:
                bstRecorder.subscriptionRecreated(event);
                break;
            case MIGRATE_BILLING:
                break;
            case CANCEL:
                bstRecorder.subscriptionCancelled(event);
                break;
            case CHANGE:
                bstRecorder.subscriptionChanged(event);
                break;
            case UNCANCEL:
                break;
            case PHASE:
                bstRecorder.subscriptionPhaseChanged(event);
                break;
            default:
                throw new RuntimeException("Unexpected event type " + event.getTransitionType());
        }
    }

    @Subscribe
    public void handleAccountCreation(final AccountCreationNotification event) {
        bacRecorder.accountCreated(event.getData());
    }

    @Subscribe
    public void handleAccountChange(final AccountChangeNotification event) {
        if (!event.hasChanges()) {
            return;
        }

        bacRecorder.accountUpdated(event.getAccountId(), event.getChangedFields());
    }

    @Subscribe
    public void handleInvoice(final InvoiceCreationNotification event) {
        bacRecorder.accountUpdated(event.getAccountId());
    }

    @Subscribe
    public void handlePaymentInfo(final PaymentInfo paymentInfo) {
        bacRecorder.accountUpdated(paymentInfo);
    }

    @Subscribe
    public void handlePaymentError(final PaymentError paymentError) {
        // TODO - we can't tie the error back to an account yet
    }
}
