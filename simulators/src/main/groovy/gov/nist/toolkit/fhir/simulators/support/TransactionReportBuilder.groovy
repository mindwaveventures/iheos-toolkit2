package gov.nist.toolkit.fhir.simulators.support

import gov.nist.toolkit.simcommon.client.SimId
import gov.nist.toolkit.simcommon.client.SimulatorConfig
import gov.nist.toolkit.simcommon.server.SimDb
import gov.nist.toolkit.transactionNotificationService.TransactionLogBean
import groovy.xml.MarkupBuilder
/**
 *
 */

class TransactionReportBuilder {

    String build(SimDb db, SimulatorConfig config) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        xml.transactionLog(type: config.actorType, simId: config.id) {
            request() {
                header(db.requestMessageHeader)
                body(new String(db.requestMessageBody))
            }
            response() {
                header(db.responseMessageHeader)
                body(new String(db.responseMessageBody))
            }
        }

        return writer.toString()
    }

    public TransactionLogBean asBean(SimDb db, SimId simId, String callbackClassName) {
        TransactionLogBean bean = new TransactionLogBean();
        bean.requestMessageHeader = db.requestMessageHeader
        bean.requestMessageBody = new String(db.requestMessageBody)
        bean.responseMessageHeader = db.responseMessageHeader
        bean.responseMessageBody = new String(db.responseMessageBody)
        bean.callbackClassName = callbackClassName
        bean.simulatorId = simId.id
        bean.simulatorUser = simId.testSession
        return bean;
    }
}
