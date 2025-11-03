// This file is auto-generated, don't edit it. Thanks.
package com.aliyun.sample;

import com.aliyun.tea.*;

public class Sample {

    /**
     * <b>description</b> :
     * <p>Initialize the Client in anonymous access</p>
     * @return Client
     * 
     * @throws Exception
     */
    public static com.aliyun.ecd20201002.Client createClient() throws Exception {
        // APIs that support anonymous access do not require authentication configuration such as AccessKey.
        // For more credentials, please refer to: https://help.aliyun.com/document_detail/378657.html.
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                .setAccessKeyId("LTAI5tKDpAnhZr52ttZfyTp7")
                .setAccessKeySecret("yPLYk8hkTCfCW0mZdbqyQ1SzAMEmAr");
        // See https://api.alibabacloud.com/product/ecd.
        config.endpoint = "ecd.us-west-1.aliyuncs.com";
        return new com.aliyun.ecd20201002.Client(config);
    }

    public static void main(String[] args_) throws Exception {
        
        com.aliyun.ecd20201002.Client client = Sample.createClient();
        com.aliyun.ecd20201002.models.GetLoginTokenRequest getLoginTokenRequest = new com.aliyun.ecd20201002.models.GetLoginTokenRequest()
                .setRegionId("us-west-1")
                .setClientId("123456789000")
                .setOfficeSiteId("us-west-1+dir-5588339126")
                .setEndUserId("hq01")
                .setPassword("Hq123456789");
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        try {
            com.aliyun.ecd20201002.models.GetLoginTokenResponse resp = client.getLoginTokenWithOptions(getLoginTokenRequest, runtime);
            com.aliyun.teaconsole.Client.log(com.aliyun.teautil.Common.toJSONString(resp));
        } catch (TeaException error) {
            System.out.println(error.getMessage());
            System.out.println(error.getData().get("Recommend"));
            com.aliyun.teautil.Common.assertAsString(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            System.out.println(error.getMessage());
            System.out.println(error.getData().get("Recommend"));
            com.aliyun.teautil.Common.assertAsString(error.message);
        }        
    }
}
