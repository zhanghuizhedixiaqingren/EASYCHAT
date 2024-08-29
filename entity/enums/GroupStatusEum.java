package com.easychat.entity.enums;

public enum GroupStatusEum {
    NORMAL(1, "正常"),
    DISSOLUTION(0, "解散");

    private Integer status;

    private String desc;

    GroupStatusEum(Integer status, String desc){
        this.status = status;
        this.desc = desc;
    }

    public static GroupStatusEum getByStatus(Integer status){
        for (GroupStatusEum item: GroupStatusEum.values()){
            if(item.getStatus().equals(status)){
                return item;
            }
        }
        return null;
    }

    public Integer getStatus() { return status; }

    public String getDesc() { return desc; }

}
