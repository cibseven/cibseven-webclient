package org.cibseven.webapp.rest;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.MultiValueMap;
import org.cibseven.webapp.rest.model.Deployment;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.providers.PermissionConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({
    @ApiResponse(responseCode = "500", description = "An unexpected system error occurred"),
    @ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController
@RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/deployment")
public class DeploymentService extends BaseService {

    /**
     * Creates a new deployment
     * 
     * @param data The deployment parameters (deployment-name, deployment-source, tenant-id, etc.)
     * @param files The files to deploy (DMN, BPMN, etc.)
     * @return The created deployment
     */
    @Operation(
        summary = "Create a new deployment",
        description = "Creates a new deployment."
                + "Requires CREATE permission on the DEPLOYMENT resource type.")
    @ApiResponse(responseCode = "200", description = "Deployment created successfully")
    @ApiResponse(responseCode = "400", description = "Bad request - invalid parameters or deployment failed")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @PostMapping("/create")
    public Deployment createDeployment(
            @RequestParam MultiValueMap<String, Object> data,
            @RequestParam(value = "data", required = true) MultipartFile[] files,
            CIBUser user) {
        // Check permissions
        checkPermission(user, SevenResourceType.DEPLOYMENT, PermissionConstants.CREATE_ALL);

        return bpmProvider.createDeployment(data, files, user);
    }
}