package chisa.zhida.knowledge;

public record MdFileView(Long id, String originalFileName, String fileSize, Integer status,
                         String createTime, String updateTime, String remark) {}
