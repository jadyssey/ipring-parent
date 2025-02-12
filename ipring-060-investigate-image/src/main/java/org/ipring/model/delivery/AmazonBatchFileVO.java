package org.ipring.model.delivery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AmazonBatchFileVO {
    private List<String> fileKeyList;
}