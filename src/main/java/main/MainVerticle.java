package main;

import crons.*;
import database.alliances.AllianceCategoryDBV;
import database.alliances.AllianceCityDBV;
import database.alliances.AllianceDBV;
import database.alliances.AllianceServiceDBV;
import database.authorizationcodes.AuthorizationCodesDBV;
import database.authservices.AuthServicesDBV;
import database.authsubservices.AuthSubServicesDBV;
import database.boardingpass.*;
import database.branchoffices.BranchofficeDBV;
import database.branchoffices.BranchofficeParcelReceivingConfigDBV;
import database.branchoffices.BranchofficeScheduleDBV;
import database.commons.DBVerticle;
import database.conekta.conektaDBV;
import database.configs.CRONConfDBV;
import database.configs.GeneralConfigDBV;
import database.configs.TimeZoneDBV;
import database.cubic.CubicDBV;
import database.cubic.CubicLogDBV;
import database.customers.CustomerAddressesDBV;
import database.customers.CustomerCaseFileDBV;
import database.customers.CustomerDBV;
import database.customers.CustomersBillingInfoDBV;
import database.ead_rad.VehicleRadEadDBV;
import database.employees.*;
import database.geo.*;
import database.health.HealthDBV;
import database.insurances.InsurancesDBV;
import database.invoicing.ComplementLetterPorteDBV;
import database.invoicing.CreditNoteDBV;
import database.invoicing.InvoiceDBV;
import database.invoicing.PaymentComplementDBV;
import database.jobs.*;
import database.module.ModuleDBV;
import database.money.*;
import database.parcel.*;
import database.permission.PermissionDBV;
import database.prepaid.PrepaidPackageDBV;
import database.prepaid.PrepaidTravelDBV;
import database.products.ProductsDBV;
import database.profile.ProfileDBV;
import database.promos.CustomersPromosDBV;
import database.promos.PromosDBV;
import database.promos.UsersPromosDBV;
import database.rental.*;
import database.routes.*;
import database.shipments.ShipmentsDBV;
import database.site.SiteDBV;
import database.ead_rad.EadRadDBV;
import database.submodule.SubModuleDBV;
import database.suppliers.SupplierBankInfoDBV;
import database.suppliers.SupplierContactDBV;
import database.suppliers.SupplierDBV;
import database.systemsVersions.systemVersionsDBV;
import database.users.UsersDBV;
import database.sellers.SellersDBV;
import database.e_wallet.EwalletDBV;
import database.admindashboard.AdminDashboardDBV;
import database.vechicle.*;
import database.segments.SegmentsDBV;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import service.alliances.AllianceCategorySV;
import service.alliances.AllianceCitySV;
import service.alliances.AllianceSV;
import service.alliances.AllianceServiceSV;
import service.authorizationcodes.AuthorizationCodesSV;
import service.authservices.AuthServicesSV;
import service.authsubservices.AuthSubServicesSV;
import service.boardingpass.*;
import service.branchoffices.BranchofficeParcelReceivingConfigSV;
import service.branchoffices.BranchofficeSV;
import service.branchoffices.BranchofficeScheduleSV;
import service.commons.*;
import service.conekta.conektaSV;
import service.configs.CRONConfSV;
import service.configs.GeneralConfigSV;
import service.configs.TimeZoneSV;
import service.cubic.CubicLogSV;
import service.cubic.CubicSV;
import service.customers.CustomerAddressesSV;
import service.customers.CustomerCaseFileSV;
import service.customers.CustomerSV;
import service.customers.CustomersBillingInfoSV;
import service.ead_rad.VehicleEadRadSV;
import service.employees.*;
import service.geo.*;
import service.health.HealthSV;
import service.insurances.InsurancesSV;
import service.invoicing.ComplementLetterPorteSV;
import service.invoicing.CreditNoteSV;
import service.invoicing.InvoiceSV;
import service.invoicing.PaymentComplementSV;
import service.jobs.*;
import service.module.ModuleSV;
import service.money.*;
import service.parcel.*;
import service.permission.PermissionSV;
import service.prepaid.PrepaidPackageSV;
import service.prepaid.PrepaidTravelSV;
import service.products.ProductsSV;
import service.profile.ProfileSV;
import service.promos.CustomersPromosSV;
import service.promos.PromosSV;
import service.promos.UsersPromosSV;
import service.rental.*;
import service.routes.*;
import service.shipments.ShipmentsSV;
import service.site.siteSV;
import service.ead_rad.EadRadSV;
import service.submodule.SubModulesSV;
import service.suppliers.SupplierBankInfoSV;
import service.suppliers.SupplierContactSV;
import service.suppliers.SupplierSV;
import service.systems.SystemsVersionsSV;
import service.users.UsersSV;
import service.sellers.SellersSV;
import service.e_wallet.EwalletSV;
import service.admindashboard.AdminDashboardSV;
import service.vechicles.*;
import service.segments.SegmentsSV;
import utils.DeploymentGroup;
import utils.UtilsDeepLinks;
import utils.UtilsRouter;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

import java.io.*;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * Main class to start all verticles
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class MainVerticle extends AbstractVerticle {

    private static final String CONFIG_FILE_PATH = "./config.json";

    private String configFilePath;

    public static DeploymentGroup deploymentGroup = DeploymentGroup.ALL;

    //constructors
    public MainVerticle() {
    }

    public MainVerticle(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        MainVerticle main;
        if (args.length > 0) {
            main = new MainVerticle(args[0]);
        } else {
            main = new MainVerticle();
        }
        Vertx v = Vertx.vertx();
        String deploymentGroupFromEnv = dotenv.get("DEPLOYMENT_GROUP");
        if (deploymentGroupFromEnv != null) {
            deploymentGroup = DeploymentGroup.valueOf(DeploymentGroup.class, deploymentGroupFromEnv.toUpperCase());
        }
        System.out.println("DEPLOYMENT GROUP: " + deploymentGroup.name());
        v.deployVerticle(main);
    }

    @Override
    public void start() throws Exception {
        //!important!//
        //initialize main router
        UtilsRouter.getInstance(vertx);

        JsonObject config = this.loadConfigFromFile();
        //systemVersions
        initializeVerticle(new systemVersionsDBV(), new SystemsVersionsSV(), config);
        //promos
        initializeVerticle(new PromosDBV(), new PromosSV(), config);
        initializeVerticle(new CustomersPromosDBV(), new CustomersPromosSV(), config);

        //sitee
        initializeVerticle(new SiteDBV(), new siteSV(), config);

        //EAD and RAD
        initializeVerticle(new EadRadDBV(), new EadRadSV(), config);
        initializeVerticle(new VehicleRadEadDBV(), new VehicleEadRadSV(), config);

        initializeVerticle(new PrepaidPackageDBV(), new PrepaidPackageSV(), config);
        initializeVerticle(new PrepaidTravelDBV(), new PrepaidTravelSV(), config);

        //profile
        initializeVerticle(new ProfileDBV(), new ProfileSV(), config);

        //users
        initializeVerticle(new UsersDBV(), new UsersSV(), config);

        //module
        initializeVerticle(new ModuleDBV(), new ModuleSV(), config);

        //sub-module
        initializeVerticle(new SubModuleDBV(), new SubModulesSV(), config);

        //permission
        initializeVerticle(new PermissionDBV(), new PermissionSV(), config);

        //authservices
        initializeVerticle(new AuthServicesDBV(), new AuthServicesSV(), config);

        //authsubservices
        initializeVerticle(new AuthSubServicesDBV(), new AuthSubServicesSV(), config);

        //AuthVerticle
        vertx.deployVerticle(new AuthVerticle(), new DeploymentOptions().setConfig(config));

        //authorization codes
        initializeVerticle(new AuthorizationCodesDBV(), new AuthorizationCodesSV(), config);

        //products
        initializeVerticle(new ProductsDBV(), new ProductsSV(), config);

        //reports
        initializeVerticle(new ReportDBV(), new ReportSV(), config);

        //conekta
        initializeVerticle(new conektaDBV(), new conektaSV(), config);

        //alliances
        initializeVerticle(new AllianceDBV(), new AllianceSV(), config);
        initializeVerticle(new AllianceServiceDBV(), new AllianceServiceSV(), config);
        initializeVerticle(new AllianceCityDBV(), new AllianceCitySV(), config);
        initializeVerticle(new AllianceCategoryDBV(), new AllianceCategorySV(), config);

        //insurances
        initializeVerticle(new InsurancesDBV(), new InsurancesSV(), config);
        initializeVerticle(new RentalDBV(), new RentalSV(), config, __ -> {
            this.vertx.deployVerticle(new CronInsurancePoliciesExpiration());
        });

        //money
        initializeVerticle(new BillDBV(), new BilSV(), config);
        initializeVerticle(new PaymentMethodDBV(), new PaymentMethodSV(), config);
        initializeVerticle(new CurrencyDBV(), new CurrencySV(), config);
        initializeVerticle(new CurrencyDenominationDBV(), new CurrencyDenominationSV(), config);
        initializeVerticle(new ExchangeRateDBV(), new ExchangeRateSV(), config);
        initializeVerticle(new ExpenseConceptDBV(), new ExpenseConceptSV(), config);
        initializeVerticle(new ExpenseSchedulesDBV(), new ExpenseScheduleSV(), config);
        initializeVerticle(new ExpenseDBV(), new ExpenseSV(), config);
        initializeVerticle(new PaymentDBV(), new PaymentSV(), config);
        initializeVerticle(new CashOutDBV(), new CashOutSV(), config);
        initializeVerticle(new CashOutDetailDBV(), new CashOutDetailSV(), config);
        initializeVerticle(new CashOutMoveDBV(), new CashOutMoveSV(), config);
        initializeVerticle(new TicketDBV(), new TicketSV(), config);
        initializeVerticle(new CashRegisterDBV(), new CashRegisterSV(), config);
        initializeVerticle(new PaybackDBV(), new PaybackSV(), config);
        initializeVerticle(new CashRegisterTicketsLogDBV(), new CashRegisterTicketsLogSV(), config);

        //bussiness jobs verticles
        initializeVerticle(new JobDBV(), new JobSV(), config);
        initializeVerticle(new RequirementDBV(), new RequirementSV(), config);
        initializeVerticle(new JobRequirementDBV(), new JobRequirementSV(), config);
        initializeVerticle(new JobFunctionDBV(), new JobFuctionSV(), config);
        initializeVerticle(new JobCaseFileDBV(), new JobCaseFileSV(), config);
        initializeVerticle(new CaseFileDBV(), new CaseFileSV(), config);

        //locations verticles
        initializeVerticle(new StreetDBV(), new StreetSV(), config);
        initializeVerticle(new CountyDBV(), new CountySV(), config);
        initializeVerticle(new SuburbDBV(), new SuburbSV(), config);
        initializeVerticle(new CityDBV(), new CitySV(), config);
        initializeVerticle(new StateDBV(), new StateSV(), config);
        initializeVerticle(new CountryDBV(), new CountrySV(), config);

        //branchoffices verticles
        initializeVerticle(new BranchofficeDBV(), new BranchofficeSV(), config);
        initializeVerticle(new BranchofficeScheduleDBV(), new BranchofficeScheduleSV(), config);
        initializeVerticle(new BranchofficeParcelReceivingConfigDBV(), new BranchofficeParcelReceivingConfigSV(), config);

        //employees verticles
        initializeVerticle(new EmployeeDBV(), new EmployeeSV(), config, __ -> {
            this.vertx.deployVerticle(new CronEmployeeAttendance());
        });
        initializeVerticle(new EmployeeContactDBV(), new EmployeeContactSV(), config);
        initializeVerticle(new EmployeeCaseFileDBV(), new EmployeeCasefileSV(), config);
        initializeVerticle(new EmployeeSchedulesDBV(), new EmployeeScheduleSV(), config);
        initializeVerticle(new EmployeeRequirementDBV(), new EmployeeRequirementSV(), config);
        initializeVerticle(new PaySheetTypeDBV(), new PaySheetTypeSV(), config);

        //vehicles        
        initializeVerticle(new ConfigVehicleDBV(), new ConfigVehicleSV(), config);
        initializeVerticle(new TypeVehicleDBV(), new TypeVehicleSV(), config);
        initializeVerticle(new VehicleDBV(), new VehicleSV(), config);
        initializeVerticle(new VehicleCaseFileDBV(), new VehicleCaseFileSV(), config);
        initializeVerticle(new VehicleCharacteristicsDBV(), new VehicleCharacteristicsSV(), config);
        initializeVerticle(new TypeServiceDBV(), new TypeServiceSV(), config);
        initializeVerticle(new CharacteristicDBV(), new CharacteristicSV(), config);
        initializeVerticle(new DriverTrackingDBV(), new DriverTrackingSV(), config);
        initializeVerticle(new TrailersDBV(), new TrailersSV(), config);

        //routes
        initializeVerticle(new ChecklistVansDBV(), new ChecklistVansSV(), config);
        initializeVerticle(new ScheduleRouteDBV(), new ScheduleRouteSV(), config);
        initializeVerticle(new ScheduleRouteDriverDBV(), new ScheduleRouteDriverSV(), config);
        initializeVerticle(new TravelIncidencesDBV(), new TravelIncidencesSV(), config);

        initializeVerticle(new ConfigDestinationDBV(), new ConfigDestinationSV(), config);
        initializeVerticle(new ConfigPriceRouteDBV(), new ConfigPriceRouteSV(), config);
        initializeVerticle(new ConfigRouteDBV(), new ConfigRouteSV(), config, __ -> {
            this.vertx.deployVerticle(new CronConfigRouteExpirationEvaluation());
        });
        initializeVerticle(new ConfigScheduleDBV(), new ConfigScheduleSV(), config);
        initializeVerticle(new ConfigTicketPriceDBV(), new ConfigTicketPriceSV(), config);
        initializeVerticle(new SpecialTicketDBV(), new SpecialTicketSV(), config);

        //boardingPasses
        initializeVerticle(new BoardingPassDBV(), new BoardingPassSV(), config);
        initializeVerticle(new BoardingPassPassengerDBV(), new BoardingPassPassengerSV(), config);
        initializeVerticle(new BoardingPassPaymentDBV(), new BoardingPassPaymentSV(), config);
        initializeVerticle(new BoardingPassRouteDBV(), new BoardingPassRouteSV(), config);
        initializeVerticle(new BoardingPassSpecialTicketDBV(), new BoardingPassSpecialTicketSV(), config);
        initializeVerticle(new BoardingPassComplementDBV(), new BoardingPassComplementSV(), config);
        initializeVerticle(new ComplementDBV(), new ComplementSV(), config);
        initializeVerticle(new ComplementIncidencesDBV(), new ComplementIncidencesSV(), config);
        initializeVerticle(new TicketPricesRulesDBV(), new TicketPricesRulesSV(), config);
        initializeVerticle(new BoardingPassDBV(), new BoardingPassSV(), config, __ -> {
            this.vertx.deployVerticle(new CronBoardingPassExpiration());
            this.vertx.deployVerticle(new CronPrepaidBoardingPassExpiration());
        });

        //luggages
        initializeVerticle(new LuggagesDBV(), new LuggagesSV(), config);
        
        //suppliers
        initializeVerticle(new SupplierDBV(), new SupplierSV(), config);
        initializeVerticle(new SupplierBankInfoDBV(), new SupplierBankInfoSV(), config);
        initializeVerticle(new SupplierContactDBV(), new SupplierContactSV(), config);

        //customers
        initializeVerticle(new CustomerDBV(), new CustomerSV(), config);
        initializeVerticle(new CustomerCaseFileDBV(), new CustomerCaseFileSV(), config);
        initializeVerticle(new CustomersBillingInfoDBV(), new CustomersBillingInfoSV(), config);
        initializeVerticle(new CustomerAddressesDBV(), new CustomerAddressesSV(), config);

        // parcels
        initializeVerticle(new PackingsDBV(), new PackingsSV(), config);
        initializeVerticle(new ComplementRuleDBV(), new ComplementRuleSV(), config);
        initializeVerticle(new PackagePriceDBV(), new PackagePriceSV(), config);
        initializeVerticle(new PackagePriceKmDBV(), new PackagePriceKmSV(), config);
        initializeVerticle(new ParcelDBV(), new ParcelSV(), config);
        initializeVerticle(new ParcelsPackagesDBV(), new ParcelsPackagesSV(), config);
        initializeVerticle(new IncidencesDBV(), new IncidencesSV(), config);
        initializeVerticle(new PackageTypesDBV(), new PackageTypesSV(), config);
        initializeVerticle(new ParcelsPackingsDBV(), new ParcelsPackingsSV(), config);
        initializeVerticle(new PetsSizesDBV(), new PetsSizesSV(), config);
        initializeVerticle(new ParcelsCancelReasonsDBV(), new ParcelsCancelReasonsSV(), config);
        initializeVerticle(new ParcelsIncidencesDBV(), new ParcelsIncidencesSV(), config);
        initializeVerticle(new PpPriceDBV(), new PpPriceSV(), config);
        initializeVerticle(new PpPriceKmDBV(), new PpPriceKmSV(), config);
        initializeVerticle(new GuiappDBV(),new GuiappSV(), config);
        initializeVerticle(new ParcelsManifestDBV(),new ParcelsManifestSV(), config);
        initializeVerticle(new DeliveryAttemptReasonDBV(),new DeliveryAttemptReasonSV(), config);

        //rentals
        initializeVerticle(new RentalDBV(), new RentalSV(), config, __ -> {
            this.vertx.deployVerticle(new CronRentalExpirationEvaluation());
        });
        initializeVerticle(new RentalEvidenceDBV(), new RentalEvidenceSV(), config);
        initializeVerticle(new RentalExtraChargeDBV(), new RentalExtraChargeSV(), config);
        initializeVerticle(new RentalConfigVehicleDBV(), new RentalConfigVehicleSV(), config);
        initializeVerticle(new AddonSerialDBV(), new AddonSerialSV(), config);
        initializeVerticle(new AddonVehicleDBV(), new AddonVehicleSV(), config);
        initializeVerticle(new ExtraChargeDBV(), new ExtraChargeSV(), config);
        initializeVerticle(new VehiclesAddonDBV(), new VehicleAddonSV(), config);
        initializeVerticle(new RentalDriverDBV(), new RentalDriverSV(), config);
        initializeVerticle(new RentalPriceDBV(), new RentalPriceSV(), config);
        initializeVerticle(new TravelTrackingDBV(), new TravelTrackingSV(), config);

        //debt payments
        initializeVerticle(new DebtPaymentDBV(), new DebtPaymentSV(), config);

        //shipments
        initializeVerticle(new ShipmentsDBV(), new ShipmentsSV(), config);
        initializeVerticle(new TravelTrackingDBV(), new TravelTrackingSV(), config);

        //invoicing
        initializeVerticle(new InvoiceDBV(), new InvoiceSV(), config);

        //time zone
        initializeVerticle(new TimeZoneDBV(), new TimeZoneSV(), config);

        //health check
        initializeVerticle(new HealthDBV(), new HealthSV(), config);

        //config
        initializeVerticle(new CRONConfDBV(), new CRONConfSV(), config, __ -> {
            this.vertx.deployVerticle(new CronExchangeRate());
        });
        initializeVerticle(new GeneralConfigDBV(), new GeneralConfigSV(), config);
        //Invoicing Complement letter porte
        initializeVerticle(new ComplementLetterPorteDBV(), new ComplementLetterPorteSV(), config);
        //mail sender
        // routes
        initializeVerticle(new ScheduleRouteDBV(), new ScheduleRouteSV(), config, __ -> {
            this.vertx.deployVerticle(new CronRouteSeatLocksExpiration());
        });
        // employees
        initializeVerticle(new EmployeeDBV(), new EmployeeSV(), config);
        // boardingpasses
        initializeVerticle(new BoardingPassDBV(), new BoardingPassSV(), config);
        // money
        initializeVerticle(new CashOutDBV(), new CashOutSV(), config);
        initializeVerticle(new PaymentDBV(), new PaymentSV(), config);
        initializeVerticle(new PaymentMethodDBV(), new PaymentMethodSV(), config);
        // promos
        initializeVerticle(new PromosDBV(), new PromosSV(), config);
        initializeVerticle(new UsersPromosDBV(), new UsersPromosSV(), config);
        initializeVerticle(new SellersDBV(), new SellersSV(), config);
        // e wallet
        initializeVerticle(new EwalletDBV(), new EwalletSV(), config);
        initializeVerticle(new AdminDashboardDBV(), new AdminDashboardSV(), config);
        initializeVerticle(new SegmentsDBV(), new SegmentsSV(), config);
        // payment complement
        initializeVerticle(new PaymentComplementDBV(), new PaymentComplementSV(), config);
        // credit note
        initializeVerticle(new CreditNoteDBV(), new CreditNoteSV(), config);
        // mails
        JsonObject mailConfig = config.getJsonObject("mail").put("mongodb", config.getJsonObject("mongodb"));
        vertx.deployVerticle(new MailVerticle(), new DeploymentOptions().setConfig(mailConfig));
        
        //file manager
        this.vertx.deployVerticle(new FileManagmentVerticle(),
                new DeploymentOptions()
                        .setConfig(config)
        );

        initializeVerticle(new CubicDBV() , new CubicSV() , config);
        initializeVerticle(new CubicLogDBV() , new CubicLogSV() , config);

        String firebaseKeyEncoded = config.getString("firebase_admin_sdk_json");
        if (firebaseKeyEncoded != null && !firebaseKeyEncoded.isEmpty()) {
            byte[] serviceAccountBytes = Base64.getDecoder().decode(firebaseKeyEncoded);

            // Initialize Firebase Admin SDK
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(serviceAccountBytes)))
                    .build();
            FirebaseApp.initializeApp(options);
        } else {
            System.out.println("No Firebase SDK json present");
        }

        String branchKey = config.getString("branch_key");
        if (branchKey != null && !branchKey.isEmpty()) {
            UtilsDeepLinks.setGlobalBranchKey(branchKey);
            UtilsDeepLinks.setGlobalWebClient(WebClient.create(vertx));
        } else {
            System.out.println("No Branch key present");
        }
    }

    /**
     * Deploy dbVerticle and if and only if the dbVerticle is successful, the
     * serviceVerticle starts, both of them runs with the object conf as
     * configuration
     *
     * @param dbVerticle data base verticle with the crud operations
     * @param serviceVerticle service verticle with the crud http services
     * @param config configuration object
     */
    public void initializeVerticle(DBVerticle dbVerticle, ServiceVerticle serviceVerticle, JsonObject config) {
        Future<String> dbVerticleDeployment = Future.future();
        vertx.deployVerticle(dbVerticle, new DeploymentOptions().setConfig(config), dbVerticleDeployment);

        dbVerticleDeployment.compose(__ -> {
            Future<String> httpVerticleDeployment = Future.future();
            vertx.deployVerticle(serviceVerticle, new DeploymentOptions().setConfig(config), httpVerticleDeployment.completer());
            return httpVerticleDeployment;
        });
    }

    /**
     * Deploy dbVerticle and if and only if the dbVerticle is successful, the
     * serviceVerticle starts, both of them runs with the object conf as
     * configuration
     *
     * @param dbVerticle data base verticle with the crud operations
     * @param serviceVerticle service verticle with the crud http services
     * @param config configuration object
     */
    public void initializeVerticle(DBVerticle dbVerticle, ServiceVerticle serviceVerticle, JsonObject config, Handler<JsonObject> finishHandler) {
        Future<String> dbVerticleDeployment = Future.future();
        vertx.deployVerticle(dbVerticle, new DeploymentOptions().setConfig(config), dbVerticleDeployment);

        dbVerticleDeployment.compose(__ -> {
            Future<String> httpVerticleDeployment = Future.future();
            vertx.deployVerticle(serviceVerticle, new DeploymentOptions().setConfig(config), httpVerticleDeployment.completer());
            return httpVerticleDeployment;
        }).compose(__ -> {
            finishHandler.handle(null);
            return Future.future();
        });
    }

    /**
     * loads into the JsonObject dbConfig the configuration from the file in db
     *
     *
     */
    private JsonObject loadConfigFromFile() throws IOException {
        if (this.configFilePath != null) {
            return this.loadConfigToJsonObject(this.configFilePath);
        } else {
            return this.loadConfigToJsonObject(CONFIG_FILE_PATH); //load the default config file for db
        }
    }

    /**
     * Loads a config file in json format to deploy verticles
     *
     * @param filePath the path of the to load
     * @return json object with the properties in the file
     * @throws FileNotFoundException if the file does not exist
     * @throws IOException if an error occuer while reading
     */
    private JsonObject loadConfigToJsonObject(final String filePath) throws IOException {
        int port = 8040;
        JsonObject result = new JsonObject()
                .put("url", "jdbc:mysql://localhost:3306/abordo_test")
                .put("auth_host", "54.190.216.23")
                .put("auth_port", 8040)
                .put("driver_class", "com.mysql.jdbc.Driver")
                .put("user", "root")
                .put("password", "12345678")
                .put("max_pool_size", 15) //the maximum number of connections to pool - default is 15
                .put("min_pool_size", 3) //the number of connections to initialise the pool with - default is 3
                .put("initial_pool_size", 3) //the minimum number of connections to pool
                .put("max_statements", 3) //the maximum number of prepared statements to cache - default is 0
                .put("max_statements_per_connection", 0) //the maximum number of prepared statements to cache per connection - default is 0
                .put("max_idle_time", 7200) //number of seconds after which an idle connection will be closed - default is 0 (never expire)
                .put(Constants.CONFIG_HTTP_SERVER_PORT, port)
                .put("web_server_hostname", "https://allabordo.com")
                .put("invoice_server_host", "api.cfdi.allabordo.com")
                .put("invoice_file_host", "media.allabordo.com/api/v1.0.0/document")
                .put("invoice_parcel_server_host", "contpaq-api-ptx-prod-150203654.us-east-1.elb.amazonaws.com")
                .put("self_server_host", "api.dev.allabordo.com")
                .put("aws_sns_access_key", "AWS_SNS_ACCESS_KEY")
                .put("aws_sns_secret_key", "AWS_SNS_SECRET_KEY")
                .put("aws_sns_topic_invoice", "FacturacionSNSTopicLocal")
                .put("aws_sns_topic_customer", "CteProveedorSNSTopicLocal")
                .put("conekta_api_key", "key_rPq4q5M52Yz8iZvMvt6tKw")
                .put("validate_period_invoice", false)
                .put("double_invoicing", false)
                .put("invoice_prefix", "DEV")
                .put("invoice_parcel_prefix", "DEV-PTX")
                .put("mail", new JsonObject()
                        .put("hostName", "mail.allabordo.com")
                        .put("port", 465)
                        .put("ssl", true)
                        .put("tls", "OPTIONAL") //DISABLED, OPTIONAL, REQUIRED
                        .put("userName", "noreply@allabordo.com")
                        .put("password", "4Tf5Q#QTn#@!")
                        .put(Constants.CONFIG_HTTP_SERVER_PORT, port)
                ).put("mongodb", new JsonObject()
                    .put("host", "localhost")
                    .put("port", 27017)
                    .put("db_name", "abordoMultimediaTest"))
                .put(Constants.GOOGLE_MAPS_API_KEY, "AIzaSyCqepbwSpvdgzj7vgQy7jKumL76CCCNH_M")
                .put("rfc_parcel", "XIA190128J61")
                .put("rfc_boardingpass", "XIA190128J61")
                .put("rfc_name_parcel", "XENON INDUSTRIAL ARTICLES")
                .put("rfc_name_boardingpass", "XENON INDUSTRIAL ARTICLES")
                .put("parcel_destination_name", "PTX PAQUETERIA")
                .put("parcel_destination_rfc", "PPA190515V16")
                .put("timbox_pwd", "79-ywLZeJyYrZJa4D9CZ")
                .put("timbox_url", "https://staging.ws.timbox.com.mx/")
                .put("boardingpass_keypem_path", "/pruebas/pasaje.key.pem")
                .put("parcel_keypem_path", "/pruebas/pasaje.key.pem")
                .put("invoice_income_base", "/pruebas/factura_ingreso_4.xml")
                .put("invoice_income_parcel", "/pruebas/factura_ingreso_4.xml")
                .put("invoice_ccp_base", "/pruebas/factura_traslado_ccp.xml")
                .put("invoice_last_mile_base", "/pruebas/factura_traslado_ultima_milla.xml")
                .put("payment_complement_base", "/pruebas/complemento_pago.xml");
        try {
            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                System.out.println("Running app with file: " + filePath + ", and the configs are:");
                result = new JsonObject(sb.toString());
            }
        } catch (FileNotFoundException e) {
            Dotenv dotenv;
            try {
                dotenv = Dotenv.configure().ignoreIfMissing().load();
                System.out.println("The file: " + filePath + ", was not found, running with environment variables:");
                result = new JsonObject()
                        .put("url", dotenv.get("API_DB_SERVER_URL"))
                        .put("auth_host", dotenv.get("API_AUTH_SERVER_HOST"))
                        .put("auth_port", Integer.valueOf(Objects.requireNonNull(dotenv.get("API_AUTH_SERVER_PORT"))))
                        .put("driver_class", dotenv.get("API_DB_SERVER_DRIVER"))
                        .put("user", dotenv.get("API_DB_SERVER_USER"))
                        .put("password", dotenv.get("API_DB_SERVER_PASSWORD"))
                        .put("max_pool_size", Integer.valueOf(Objects.requireNonNull(dotenv.get("API_DB_SERVER_MAX_POOL_SIZE")))) //the maximum number of connections to pool - default is 15
                        .put("min_pool_size", Integer.valueOf(Objects.requireNonNull(dotenv.get("API_DB_SERVER_MIN_POOL_SIZE")))) //the number of connections to initialise the pool with - default is 3
                        .put("initial_pool_size", Integer.valueOf(Objects.requireNonNull(dotenv.get("API_DB_SERVER_INI_POOL_SIZE")))) //the minimum number of connections to pool
                        .put("max_statements", Integer.valueOf(Objects.requireNonNull(dotenv.get("API_DB_SERVER_MAX_STATEMENTS")))) //the maximum number of prepared statements to cache - default is 0
                        .put("max_statements_per_connection", Integer.valueOf(Objects.requireNonNull(dotenv.get("API_DB_SERVER_MAX_STATEMENTS_PER_CONNECTION")))) //the maximum number of prepared statements to cache per connection - default is 0
                        .put("max_idle_time", Integer.valueOf(Objects.requireNonNull(dotenv.get("API_DB_SERVER_MAX_IDLE_TIME")))) //number of seconds after which an idle connection will be closed - default is 0 (never expire)
                        .put(Constants.CONFIG_HTTP_SERVER_PORT, Integer.valueOf(Objects.requireNonNull(dotenv.get("API_HTTP_SERVER_PORT"))))
                        .put("web_server_hostname", dotenv.get("WEB_SERVER_HOSTNAME"))
                        .put("invoice_server_host", dotenv.get("INVOICE_SERVER_HOST"))
                        .put("invoice_file_host", dotenv.get("INVOICE_FILE_HOST"))
                        .put("invoice_parcel_server_host", dotenv.get("INVOICE_PARCEL_SERVER_HOST"))
                        .put("self_server_host", dotenv.get("SELF_SERVER_HOST"))
                        .put("aws_sns_access_key", dotenv.get("AWS_SNS_ACCESS_KEY"))
                        .put("aws_sns_secret_key", dotenv.get("AWS_SNS_SECRET_KEY"))
                        .put("aws_sns_topic_invoice", dotenv.get("SNS_TOPIC_INVOICE"))
                        .put("aws_sns_topic_customer", dotenv.get("SNS_TOPIC_CUSTOMER"))
                        .put("conekta_api_key", dotenv.get("CONEKTA_API_KEY"))
                        .put("validate_period_invoice", Boolean.valueOf(dotenv.get("VALIDATE_PERIOD_INVOICE")))
                        .put("register_invoices", Boolean.valueOf(dotenv.get("REGISTER_INVOICES")))
                        .put("double_invoicing", Boolean.valueOf(dotenv.get("DOUBLE_INVOICING")))
                        .put("invoice_prefix", dotenv.get("INVOICE_PREFIX"))
                        .put("customer_invoice_prefix", dotenv.get("CUSTOMER_INVOICE_PREFIX"))
                        .put("invoice_parcel_prefix", dotenv.get("INVOICE_PARCEL_PREFIX"))
                        .put("mail", new JsonObject()
                                .put("hostName", dotenv.get("API_MAIL_HOSTNAME"))
                                .put("port", Integer.valueOf(Objects.requireNonNull(dotenv.get("API_MAIL_PORT"))))
                                .put("ssl", Boolean.valueOf(dotenv.get("API_MAIL_SSL")))
                                .put("tls", dotenv.get("API_MAIL_TLS")) //DISABLED, OPTIONAL, REQUIRED
                                .put("userName", dotenv.get("API_MAIL_USERNAME"))
                                .put("password", dotenv.get("API_MAIL_PASSWORD"))
                                .put(Constants.CONFIG_HTTP_SERVER_PORT, Integer.valueOf(Objects.requireNonNull(dotenv.get("API_HTTP_SERVER_PORT")))))
                        .put("mongodb", new JsonObject()
                                .put("host", dotenv.get("MONGODB_HOST"))
                                .put("port", Integer.valueOf(Objects.requireNonNull(dotenv.get("MONGODB_PORT"))))
                                .put("db_name", dotenv.get("MONGODB_NAME")))
                        .put(Constants.GOOGLE_MAPS_API_KEY, dotenv.get("GOOGLE_MAPS_API_KEY"))
                        .put("rfc_parcel", dotenv.get("RFC_PARCEL"))
                        .put("rfc_boardingpass", dotenv.get("RFC_BOARDINGPASS"))
                        .put("rfc_name_parcel", dotenv.get("RFC_NAME_PARCEL"))
                        .put("rfc_name_boardingpass", dotenv.get("RFC_NAME_BOARDINGPASS"))
                        .put("parcel_destination_name", dotenv.get("PARCEL_DESTINATION_NAME"))
                        .put("parcel_destination_rfc", dotenv.get("PARCEL_DESTINATION_RFC"))
                        .put("timbox_pwd", dotenv.get("TIMBOX_PWD"))
                        .put("timbox_url", dotenv.get("TIMBOX_URL"))
                        .put("boardingpass_keypem_path", dotenv.get("BOARDINGPASS_KEYPEM_PATH"))
                        .put("parcel_keypem_path", dotenv.get("PARCEL_KEYPEM_PATH"))
                        .put("invoice_income_base", dotenv.get("INVOICE_INCOME_BASE"))
                        .put("invoice_income_parcel", dotenv.get("INVOICE_INCOME_PARCEL"))
                        .put("firebase_admin_sdk_json", dotenv.get("FIREBASE_ADMIN_SDK_JSON"))
                        .put("branch_key", dotenv.get("BRANCH_KEY"))
                        .put("invoice_ccp_base", dotenv.get("INVOICE_CCP_BASE"))
                        .put("invoice_last_mile_base", dotenv.get("INVOICE_LAST_MILE_BASE"))
                        .put("payment_complement_base", dotenv.get("PAYMENT_COMPLEMENT_BASE"));
                return result;
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }
}
