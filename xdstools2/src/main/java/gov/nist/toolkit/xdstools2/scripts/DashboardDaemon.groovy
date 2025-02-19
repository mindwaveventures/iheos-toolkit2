package gov.nist.toolkit.xdstools2.scripts

import gov.nist.toolkit.actortransaction.shared.ActorType
import gov.nist.toolkit.configDatatypes.client.TransactionType
import gov.nist.toolkit.installation.server.Installation
import gov.nist.toolkit.installation.shared.TestSession
import gov.nist.toolkit.registrymetadata.Metadata
import gov.nist.toolkit.registrymetadata.MetadataParser
import gov.nist.toolkit.results.client.LogIdIOFormat
import gov.nist.toolkit.results.client.LogIdType
import gov.nist.toolkit.results.client.TestInstance
import gov.nist.toolkit.session.server.Session
import gov.nist.toolkit.sitemanagement.SeparateSiteLoader
import gov.nist.toolkit.sitemanagement.Sites
import gov.nist.toolkit.sitemanagement.client.Site
import gov.nist.toolkit.sitemanagement.client.SiteSpec
import gov.nist.toolkit.testengine.engine.TransactionSettings
import gov.nist.toolkit.testengine.engine.Xdstest2
import gov.nist.toolkit.testenginelogging.client.LogFileContentDTO
import gov.nist.toolkit.testenginelogging.client.LogMapDTO
import gov.nist.toolkit.testenginelogging.client.LogMapItemDTO
import gov.nist.toolkit.testenginelogging.client.TestStepLogContentDTO
import gov.nist.toolkit.testenginelogging.logrepository.LogRepository
import gov.nist.toolkit.testenginelogging.logrepository.LogRepositoryFactory
import gov.nist.toolkit.testkitutilities.TestKit
import gov.nist.toolkit.testkitutilities.TestKitSearchPath
import gov.nist.toolkit.utilities.xml.Util
import gov.nist.toolkit.utilities.xml.XmlUtil
import gov.nist.toolkit.xdsexception.ExceptionUtil
import gov.nist.toolkit.xdstools2.shared.RegistryStatus
import gov.nist.toolkit.xdstools2.shared.RepositoryStatus
import groovy.transform.TypeChecked
import org.apache.axiom.om.OMElement

import javax.xml.parsers.FactoryConfigurationError

@TypeChecked
public class DashboardDaemon {
//	ToolkitServiceImpl toolkit = new ToolkitServiceImpl();
	String pid = "a1e4655b50754a2^^^&1.3.6.1.4.1.21367.2005.3.7&ISO";
	String output = "/Users/bill/tmp/dashboard";
	Sites sites;
	Session s;
	String environmentName;
	String warHome;
	File externalCache;
	List<RepositoryStatus> repositories = new ArrayList<RepositoryStatus>();
	Date date = new Date();
    static final int exceptionReportingDepth = 15
	TestSession testSession = TestSession.DEFAULT_TEST_SESSION

	public File getDashboardDirectory() {
		return new File(output);
	}

	public Session getSession() {
		return s;
	}

	public DashboardDaemon(String warHome, String outputDirStr, String environment, String externalCache)  {
		this.environmentName = environment;
		this.warHome = warHome;
		this.externalCache = new File(externalCache)
//		toolkit.setWarHome(warHome);
		s = new Session(new File(warHome));
		try {
			s.setEnvironment(environmentName, externalCache);
		} catch (Exception e) {
			System.out.println("Cannot set environment: " + e.getMessage());
			System.exit(-1);
		}
		output = outputDirStr;
	}

	public void run(String pid, boolean secure) throws FactoryConfigurationError, Exception, IOException {
		this.pid = pid;
		new File(output).mkdirs();
//		SiteServiceManager siteServiceManager = new SiteServiceManager(null);
//		siteServiceManager.loadExternalSites();
		sites = null;
		sites = new SeparateSiteLoader(testSession).load(Installation.instance().actorsDir(testSession), sites);
        println 'sites are ' + sites
//		sites = siteServiceManager.getSites();
		// experimental
//		s = toolkit.getSession();
		s.setTls(secure);    // ignores this and still uses TLS
		scanRepositories(secure);
		scanRegistries(secure);
	}

	void scanRegistries(boolean secure)  {
		File dir = new File(output + "/Registry");
		dir.mkdirs();

		List<String> registrySiteNames;
		try {
			registrySiteNames = sites.getSiteNamesWithActor(ActorType.REGISTRY, testSession);
			System.out.println("Registry sites are " + registrySiteNames);
		} catch (Exception e1) {
			System.out.println("Exception: " + e1.getMessage());
			return;
		}
		for (String regSiteName : registrySiteNames) {
			RegistryStatus regStatus = new RegistryStatus();
			SiteSpec siteSpec = new SiteSpec(regSiteName, ActorType.REGISTRY, null, testSession);
			regStatus.name = siteSpec.name;
			Site site;
			try {
				site = sites.getSite(siteSpec.name, testSession);
			} catch (Exception e1) {
				regStatus.status = false;
				regStatus.fatalError = ExceptionUtil.exception_details(e1, exceptionReportingDepth);
				registrySave(regStatus, new File(dir, regSiteName + ".ser"))
				continue;
			}
			try {
				regStatus.endpoint = site.getEndpoint(TransactionType.STORED_QUERY, secure, false);
			} catch (Exception e1) {
				regStatus.status = false;
				regStatus.fatalError = ExceptionUtil.exception_details(e1, exceptionReportingDepth);
				registrySave(regStatus, new File(dir, regSiteName + ".ser"))
				continue;
			}

			TestKitSearchPath searchPath = new TestKitSearchPath(environmentName, testSession);
			Xdstest2 xdstest;
			try {
				xdstest = new Xdstest2(new File(warHome + File.separator + "toolkitx"), searchPath, null, testSession);
			} catch (Exception e) {
				regStatus.status = false;
				regStatus.fatalError = ExceptionUtil.exception_details(e, exceptionReportingDepth);
				registrySave(regStatus, new File(dir, regSiteName + ".ser"))
				continue;
			}
			xdstest.setSites(sites);
			xdstest.setSite(site);
			xdstest.setSecure(secure);
            TestInstance testInstance = new TestInstance("GetDocuments", TestSession.DEFAULT_TEST_SESSION)
			List<String> areas = ['utilities'];
			List<String> sections = new ArrayList<String>();
			sections.add("XDS");
			try {
				TestKit testKit = searchPath.getTestKitForTest(testInstance.getId());
				if (testKit == null)
					throw new Exception("Test " + testInstance + " not found");
				xdstest.addTest(testKit, testInstance, sections, (String[]) areas.toArray());
			} catch (Exception e1) {
				regStatus.status = false;
				regStatus.fatalError = ExceptionUtil.exception_details(e1, exceptionReportingDepth);
				registrySave(regStatus, new File(dir, regSiteName + ".ser"))
				continue;
			}
			Map<String, String> parms = new HashMap<String, String>();
			parms.put('$returnType$', "ObjectRef");
			int idx=0;
			for (RepositoryStatus repStat : repositories) {
				if (repStat.docId == null)
					continue;
				parms.put('$id' + String.valueOf(idx) + '$', repStat.docId);
				idx++;
			}
			TransactionSettings ts = new TransactionSettings();
			ts.assignPatientId = false;
			ts.siteSpec = new SiteSpec(testSession);
			ts.siteSpec.isAsync = false;
			ts.securityParams = s;
            ts.logRepository =
            LogRepositoryFactory.
                    getLogRepository(
                            Installation.instance().sessionCache(),
                            session.getId(),
                            LogIdIOFormat.JAVA_SERIALIZATION,
                            LogIdType.TIME_ID,
                            null)
            xdstest.setLogRepository(ts.logRepository)
            try {
				xdstest.run(parms, null, true, ts);
			} catch (Exception e1) {
				regStatus.status = false;
				regStatus.fatalError = ExceptionUtil.exception_details(e1, exceptionReportingDepth);
				registrySave(regStatus, new File(dir, regSiteName + ".ser"))
				continue;
			}
			LogMapDTO logMap;
			try {
				logMap = xdstest.getLogMap();
			} catch (Exception e1) {
				regStatus.status = false;
				regStatus.fatalError = ExceptionUtil.exception_details(e1, exceptionReportingDepth)
				registrySave(regStatus, new File(dir, regSiteName + ".ser"))
				continue;
			}
			LogMapItemDTO item = logMap.getItems().get(0);
			LogFileContentDTO logFile = item.log;
			List<TestStepLogContentDTO> testStepLogs;
			try {
				testStepLogs = logFile.getStepLogs();
			} catch (Exception e1) {
				regStatus.status = false;
				regStatus.fatalError = ExceptionUtil.exception_details(e1, exceptionReportingDepth)
				registrySave(regStatus, new File(dir, regSiteName + ".ser"))
				continue;
			}
			TestStepLogContentDTO testStepLogContentDTO = testStepLogs.get(0);

			try {
				OMElement ele = Util.parse_xml(testStepLogContentDTO.getResult());
				List<OMElement> objrefs = XmlUtil.decendentsWithLocalName(ele, "ObjectRef");
				Metadata m = new Metadata();
				for (OMElement objref : objrefs) {
					String id = m.getId(objref);
					for (RepositoryStatus repStat : repositories) {
						if (id.equals(repStat.docId)) {
							repStat.registry = regSiteName;
							repositorySave(repStat);

						}
					}
				}
			} catch (Exception e) {

			}

			regStatus.status = logFile.isSuccess();
			regStatus.fatalError = logFile.getFatalError();

			try {
				regStatus.errors = testStepLogContentDTO.getErrors();
			} catch (Exception e) {
			}

			registrySave(regStatus, new File(dir, regSiteName + ".ser"))


		}
	}

	void scanRepositories(boolean secure)  {
		List<String> repositorySiteNames;
		try {
			repositorySiteNames = sites.getSiteNamesWithRepository(testSession);
			System.out.println("Repository Sites are " + repositorySiteNames);
		} catch (Exception e1) {
			System.out.println("Exception: " + e1.getMessage());
			return;
		}
		for (String repSiteName : repositorySiteNames) {
			RepositoryStatus rstatus = new RepositoryStatus();
			repositories.add(rstatus);
			rstatus.date = date.toString();
			SiteSpec siteSpec = new SiteSpec(repSiteName, ActorType.REPOSITORY, null, testSession);
			rstatus.name = siteSpec.name;
			Site site;
			try {
				site = sites.getSite(siteSpec.name, testSession);
			} catch (Exception e1) {
				rstatus.status = false;
				rstatus.fatalError = ExceptionUtil.exception_details(e1, exceptionReportingDepth);
				repositorySave(rstatus);
				continue;
			}
			try {
				rstatus.endpoint = site.getEndpoint(TransactionType.PROVIDE_AND_REGISTER, secure, false);
			} catch (Exception e) {
				rstatus.status = false;
				rstatus.fatalError = ExceptionUtil.exception_details(e, exceptionReportingDepth)
				repositorySave(rstatus);
				continue;
			}

			System.out.println("PnR endpoint: " + rstatus.endpoint);

			Xdstest2 xdstest;
			try {
				TestKitSearchPath searchPath = new TestKitSearchPath(environmentName, testSession);
				xdstest = new Xdstest2(new File(warHome + File.separator + "toolkitx"), searchPath, null, testSession);
			} catch (Exception e) {
				rstatus.status = false;
				rstatus.fatalError = ExceptionUtil.exception_details(e, exceptionReportingDepth)
				repositorySave(rstatus);
				continue;
			}
			xdstest.setSites(sites);
			xdstest.setSite(site);
			xdstest.setSecure(secure);
            TestInstance testInstance = new TestInstance("SingleDocument-Repository", TestSession.DEFAULT_TEST_SESSION)
            LogRepository logRepository = LogRepositoryFactory.
                    getLogRepository(
                            Installation.instance().sessionCache(),
                            session.getId(),
                            LogIdIOFormat.JAVA_SERIALIZATION,
                            LogIdType.TIME_ID,
                            null)
            xdstest.setLogRepository(logRepository)
            println logRepository
            List<String> areas = ['testdata-repository']
			try {
				TestKitSearchPath searchPath = new TestKitSearchPath(environmentName, testSession);
				TestKit testKit = searchPath.getTestKitForTest(testInstance.getId());
				if (testKit == null)
					throw new Exception("Test " + testInstance + " not found");
				xdstest.addTest(testKit, testInstance, null, (String[]) areas.toArray());
			} catch (Exception e1) {
				rstatus.status = false;
				rstatus.fatalError = ExceptionUtil.exception_details(e1, exceptionReportingDepth)
				repositorySave(rstatus);
				continue;
			}
			Map<String, String> parms = new HashMap<String, String>();
			parms.put('$patientid$', pid);
			TransactionSettings ts = new TransactionSettings();
			ts.siteSpec = new SiteSpec(testSession);
			ts.assignPatientId = false;
			ts.siteSpec.isAsync = false;
			ts.securityParams = s;
            ts.logRepository = logRepository
			try {
				xdstest.run(parms, null, true, ts);
			} catch (Exception e1) {
				rstatus.status = false;
				rstatus.fatalError = ExceptionUtil.exception_details(e1, exceptionReportingDepth)
				repositorySave(rstatus);
				continue;
			}
			LogMapDTO logMap;
			try {
				logMap = xdstest.getLogMap();
			} catch (Exception e1) {
				rstatus.status = false;
				rstatus.fatalError = ExceptionUtil.exception_details(e1, exceptionReportingDepth)
				repositorySave(rstatus);
				continue;
			}
			LogMapItemDTO item = logMap.getItems().get(0);
			LogFileContentDTO logFile = item.log;
			List<TestStepLogContentDTO> testStepLogs;
			try {
				testStepLogs = logFile.getStepLogs();
			} catch (Exception e1) {
				rstatus.status = false;
				rstatus.fatalError = ExceptionUtil.exception_details(e1, exceptionReportingDepth)
				repositorySave(rstatus);
				continue;
			}
			TestStepLogContentDTO stepLogContentDTO = testStepLogs.get(0);

			try {
				OMElement ele = Util.parse_xml(stepLogContentDTO.getInputMetadata());
				Metadata m = MetadataParser.parseNonSubmission(ele);
				OMElement de = m.getExtrinsicObject(0);
				String docUUID = m.getId(de);
				rstatus.docId = docUUID;
			} catch (Exception e) {

			}

			rstatus.status = logFile.isSuccess();
			rstatus.fatalError = logFile.getFatalError();

			System.out.println("Fatal error is " + rstatus.fatalError);

			try {
				rstatus.errors = stepLogContentDTO.getErrors();
			} catch (Exception e) {
			}


			repositorySave(rstatus);

		}
	}

	void repositorySave(RepositoryStatus repStat)  {
        println repStat
		File dir = new File(output + "/Repository");
		dir.mkdirs();

		String repSiteName = repStat.name;

		String filename = new File(dir, repSiteName + ".ser");

		FileOutputStream fos = null;
		ObjectOutputStream out = null;

		try {
			fos = new FileOutputStream(filename);
			out = new ObjectOutputStream(fos);
			out.writeObject(repStat);
			out.close();
		} catch (IOException e) {
			System.out.println("ERROR: cannot write out results");
		}

	}

	void registrySave(RegistryStatus regStat, File outputFile)  {
        println regStat
		FileOutputStream fos = null;
		ObjectOutputStream out = null;

		try{
			fos = new FileOutputStream(outputFile);
			out = new ObjectOutputStream(fos);
			out.writeObject(regStat);
			out.close();
		} catch (IOException e) {
			System.out.println("ERROR: cannot write out results");
		}

	}

	static public void main(String[] args) {

		if (args.length != 6) {
			System.out.println("Usage: DashboardDaemon <Patient ID> <warHome> <output directory> <environment_name> <external_cache> <TLS? (true|false)>");
			System.exit(-1);
		}

		String pid = args[0];
		String warhom = args[1];
		String outdir = args[2];
		String env = args[3];
		String externalCache = args[4];
		boolean secure = 'true' == args[5]

		try {
			DashboardDaemon dd = new DashboardDaemon(warhom, outdir, env, externalCache);
			dd.run(pid, secure);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
