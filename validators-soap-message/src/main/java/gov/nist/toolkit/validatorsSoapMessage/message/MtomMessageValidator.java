package gov.nist.toolkit.validatorsSoapMessage.message;

import gov.nist.toolkit.docref.Mtom;
import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.errorrecording.client.XdsErrorCode;
import gov.nist.toolkit.errorrecording.factories.ErrorRecorderBuilder;
import gov.nist.toolkit.http.HttpParseException;
import gov.nist.toolkit.http.HttpParserBa;
import gov.nist.toolkit.http.MultipartParserBa;
import gov.nist.toolkit.http.PartBa;
import gov.nist.toolkit.installation.shared.TestSession;
import gov.nist.toolkit.validatorsSoapMessage.factories.SoapMessageValidatorFactory;
import gov.nist.toolkit.valregmsg.message.MultipartContainer;
import gov.nist.toolkit.valsupport.client.ValidationContext;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import gov.nist.toolkit.valsupport.message.AbstractMessageValidator;
import gov.nist.toolkit.valsupport.registry.RegistryValidationInterface;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class MtomMessageValidator extends AbstractMessageValidator {
	HttpParserBa headers;
	ErrorRecorderBuilder erBuilder;
	MessageValidatorEngine mvc;
	RegistryValidationInterface rvi;
	byte[] bodyBytes;
	TestSession testSession;

	public MtomMessageValidator(ValidationContext vc, HttpParserBa headers, byte[] body, ErrorRecorderBuilder erBuilder, MessageValidatorEngine mvc, RegistryValidationInterface rvi, TestSession testSession) {
		super(vc);
		this.headers = headers;
		this.erBuilder = erBuilder;
		this.mvc = mvc;
		this.rvi = rvi;
		this.bodyBytes = body;
		this.testSession = testSession;
	}


	public void run(ErrorRecorder er, MessageValidatorEngine mvc) {
		this.er = er;
		er.registerValidator(this);
		headers.setErrorRecorder(er);
		try {
			

			HttpParserBa hp = headers;
			
			String body = new String(bodyBytes, hp.getCharset());
			hp.setBody(bodyBytes);
			hp.tryMultipart();
			MultipartParserBa mp = hp.getMultipartParser();
			

			er.detail("Multipart contains " + mp.getPartCount() + " parts");
			if (mp.getPartCount() == 0) {
				er.err(XdsErrorCode.Code.NoCode, "Cannot continue parsing, no Parts found", this, "");
				er.unRegisterValidator(this);
				return;
			}
			
			List<String> partIds = new ArrayList<String>();
			for (int i=0; i<mp.getPartCount(); i++) {
				PartBa p = mp.getPart(i);
				partIds.add(p.getContentId());
			}
			er.detail("Part Content-IDs are " + partIds);
			
			
			PartBa startPart = mp.getStartPart();
			
			if (startPart != null)
				er.detail("Found start part - " + startPart.getContentId());
			else {
				er.err(XdsErrorCode.Code.NoCode, "Start part [" + mp.getStartPartId() + "] not found", this, Mtom.XOP_example2);
				er.unRegisterValidator(this);
				return;
			}
				
			vc.isSimpleSoap = false;

			// no actual validation, just saves Part list on validation stack so it can
			// be found by later steps that need it
			mvc.addMessageValidator("MultipartContainer", new MultipartContainer(vc, mp), erBuilder.buildNewErrorRecorder());

			
			er.detail("Scheduling validation of SOAP wrapper");
			SoapMessageValidatorFactory.getValidatorContext(erBuilder, startPart.getBody(), mvc, "Validate SOAP", vc, rvi, testSession);

		} catch (UnsupportedEncodingException e) {
			er.err(XdsErrorCode.Code.NoCode, e);
		} catch (HttpParseException e) {
			er.err(XdsErrorCode.Code.NoCode, e);
		}
        finally {
            er.unRegisterValidator(this);
        }

	}

	@Override
	public boolean isSystemValidator() { return true; }

}
