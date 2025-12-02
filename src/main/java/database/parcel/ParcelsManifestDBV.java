package database.parcel;

import database.commons.DBVerticle;
import database.parcel.handlers.ParcelsManifestDBV.*;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import static service.commons.Constants.ACTION;

public class ParcelsManifestDBV extends DBVerticle {

    public static final String ACTION_REGISTER = "ParcelsManifestDBV.register";
    public static final String ACTION_OPEN_LIST = "ParcelsManifestDBV.openList";
    public static final String ACTION_CHECK_EAD = "ParcelsManifestDBV.checkEad";
    public static final String ACTION_CLOSE = "ParcelsManifestDBV.close";
    public static final String ACTION_INIT_ROUTE_EAD = "ParcelsManifestDBV.initRouteEad";
    public static final String ACTION_FINISH = "ParcelsManifestDBV.finish";
    public static final String ACTION_GET_REPORT = "ParcelsManifestDBV.getReport";
    public static final String ACTION_GET_REPORT_DETAILS = "ParcelsManifestDBV.getReportDetails";
    public static final String ACTION_CHECK_EAD_CONTINGENCY = "ParcelsManifestDBV.checkEadContingency";
    public static final String ACTION_GET_DETAIL = "ParcelsManifestDBV.getDetail";
    public static final String ACTION_DELETE_PARCEL_MANIFEST_DETAIL = "ParcelsManifestDBV.deleteParcelManifestDetail";
    public static final String ACTION_RETURN_PACKAGE_TO_ARRIVED = "ParcelsManifestDBV.returnPackageToArrived";
    public static final String ACTION_GET_LOGS = "ParcelsManifestDBV.getLogs";
    public static final String ACTION_GET_LOG_DETAILS = "ParcelsManifestDBV.getLogDetails";
    public static final String ACTION_FINISH_CONTINGENCY = "ParcelsManifestDBV.finishContingency";
    public static final String ACTION_DELIVERY_ATTEMPT = "ParcelsManifestDBV.deliveryAttempt";
    public static final String ACITON_ROUTE_LOG = "ParcelsManifestDBV.routeLog";
    public static final String ACTION_GET_ROUTE_LOG = "ParcelsManifestDBV.getRouteLog";
    public static final String ACTION_GET_ROUTE_LOG_DETAIL = "ParcelsManifestDBV.getRouteDetailLog";

    @Override
    public String getTableName() {
        return "parcels_manifest";
    }

    Register register;
    OpenList openList;
    CheckEad checkEad;
    Close close;
    InitRouteEad initRouteEad;
    Finish finish;
    Report report;
    ReportDetails reportDetails;
    CheckEadContingency checkEadContingency;
    Detail detail;
    DeleteCP deleteCP;
    ReturnPackageToArrived returnPackageToArrived;
    Logs logs;
    LogDetails logDetails;
    FinishContingency finishContingency;
    DeliveryAttempt deliveryAttempt;
    RouteLog routeLog;
    RouteLogReport routeLogReport;
    RouteLogDetailReport routeLogDetailReport;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        this.register = new Register(this);
        this.openList = new OpenList(this);
        this.checkEad = new CheckEad(this);
        this.close = new Close(this);
        this.initRouteEad = new InitRouteEad(this);
        this.finish = new Finish(this);
        this.report = new Report(this);
        this.reportDetails = new ReportDetails(this);
        this.checkEadContingency = new CheckEadContingency(this);
        this.detail = new Detail(this);
        this.deleteCP = new DeleteCP(this);
        this.returnPackageToArrived = new ReturnPackageToArrived(this);
        this.logs = new Logs(this);
        this.logDetails = new LogDetails(this);
        this.finishContingency = new FinishContingency(this);
        this.routeLog = new RouteLog(this);
        this.routeLogReport = new RouteLogReport(this);
        this.routeLogDetailReport = new RouteLogDetailReport(this);
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_REGISTER:
                this.register.handle(message);
                break;
            case ACTION_OPEN_LIST:
                this.openList.handle(message);
                break;
            case ACTION_CHECK_EAD:
                this.checkEad.handle(message);
                break;
            case ACTION_CLOSE:
                this.close.handle(message);
                break;
            case ACTION_INIT_ROUTE_EAD:
                this.initRouteEad.handle(message);
                break;
            case ACTION_FINISH:
                this.finish.handle(message);
                break;
            case ACTION_GET_REPORT:
                this.report.handle(message);
                break;
            case ACTION_GET_REPORT_DETAILS:
                this.reportDetails.handle(message);
                break;
            case ACTION_CHECK_EAD_CONTINGENCY:
                this.checkEadContingency.handle(message);
                break;
            case ACTION_GET_DETAIL:
                this.detail.handle(message);
                break;
            case ACTION_DELETE_PARCEL_MANIFEST_DETAIL:
                this.deleteCP.handle(message);
                break;
            case ACTION_RETURN_PACKAGE_TO_ARRIVED:
                this.returnPackageToArrived.handle(message);
                break;
            case ACTION_GET_LOGS:
                this.logs.handle(message);
                break;
            case ACTION_GET_LOG_DETAILS:
                this.logDetails.handle(message);
                break;
            case ACTION_FINISH_CONTINGENCY:
                this.finishContingency.handle(message);
                break;
            case ACTION_DELIVERY_ATTEMPT:
                this.deliveryAttempt.handle(message);
                break;
            case ACITON_ROUTE_LOG:
                this.routeLog.handle(message);
                break;
            case ACTION_GET_ROUTE_LOG:
                this.routeLogReport.handle(message);
                break;
            case ACTION_GET_ROUTE_LOG_DETAIL:
                this.routeLogDetailReport.handle(message);
                break;
        }
    }
}
