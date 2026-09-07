package chisa.zhida.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("t_ai_customer_service_md_storage")
public class KnowledgeFileEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String md5;
    private String originalFileName;
    private String storagePath;
    private Long fileSize;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public String getMd5() { return md5; }
    public void setMd5(String value) { this.md5 = value; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String value) { this.originalFileName = value; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String value) { this.storagePath = value; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long value) { this.fileSize = value; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer value) { this.status = value; }
    public String getRemark() { return remark; }
    public void setRemark(String value) { this.remark = value; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime value) { this.createTime = value; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime value) { this.updateTime = value; }
}
