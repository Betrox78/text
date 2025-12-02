package database.commons;

public class ExportGlobal {

    private Double total;
    private String code;
    private String created_at;


    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public ExportGlobal(Double total, String code, String created_at){
        this.total = total;
        this.code = code;
        this.created_at = created_at;
    }

}
