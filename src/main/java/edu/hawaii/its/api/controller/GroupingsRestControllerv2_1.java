package edu.hawaii.its.api.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.hawaii.its.api.service.GroupAttributeService;
import edu.hawaii.its.api.service.GroupingAssignmentService;
import edu.hawaii.its.api.service.HelperService;
import edu.hawaii.its.api.service.MemberAttributeService;
import edu.hawaii.its.api.service.MembershipService;
import edu.hawaii.its.api.type.AdminListsHolder;
import edu.hawaii.its.api.type.GenericServiceResult;
import edu.hawaii.its.api.type.Grouping;
import edu.hawaii.its.api.type.GroupingsServiceResult;
import edu.hawaii.its.api.type.Membership;
import edu.hawaii.its.api.type.SyncDestination;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groupings/v2.1")
public class GroupingsRestControllerv2_1 {

    private static final Log logger = LogFactory.getLog(GroupingsRestControllerv2_1.class);

    @Value("${app.groupings.controller.uuid}")
    private String uuid;

    @Value("${app.iam.request.form}")
    private String requestForm;

    @Value("${groupings.api.exclude}")
    private String EXCLUDE;

    @Value("${groupings.api.include}")
    private String INCLUDE;

    @Value("${groupings.api.opt_in}")
    private String OPT_IN;

    @Value("${groupings.api.opt_out}")
    private String OPT_OUT;

    @Value("${groupings.api.listserv}")
    private String LISTSERV;

    @Value("${groupings.api.releasedgrouping}")
    private String RELEASED_GROUPING;

    @Autowired
    private GroupAttributeService groupAttributeService;

    @Autowired
    private GroupingAssignmentService groupingAssignmentService;

    @Autowired
    private MemberAttributeService memberAttributeService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private HelperService helperService;

    @PostConstruct
    public void init() {
        Assert.hasLength(uuid, "Property 'app.groupings.controller.uuid' is required.");
        logger.info("GroupingsRestController started.");
    }

    @GetMapping(value = "/")
    @ResponseBody public ResponseEntity hello() {
        return ResponseEntity
                .ok()
                .body("University of Hawaii Groupings");
    }

    @GetMapping(value = "/generic")
    public ResponseEntity<GenericServiceResult> generic() {
        return ResponseEntity
                .ok()
                .body(membershipService.generic());
    }

    @GetMapping(value = "/swagger/toString/{path}")
    @ResponseBody
    public ResponseEntity<GenericServiceResult> swaggerToString(@RequestHeader("current_user") String currentUser,
            String path) {
        logger.info("Entered REST swaggerToString");
        return ResponseEntity
                .ok()
                .body(helperService.swaggerToString(currentUser, path));
    }

    /**
     * Get all admins and groupings
     *
     * @return List of all admins and all groupings
     */
    @GetMapping(value = "/adminsGroupings")
    @ResponseBody
    public ResponseEntity<AdminListsHolder> adminsGroupings(@RequestHeader("current_user") String currentUser) {
        logger.info("Entered REST adminsGroupings...");
        return ResponseEntity
                .ok()
                .body(groupingAssignmentService.adminLists(currentUser));
    }

    /**
     * Create a new admin
     *
     * @param uid: uid of admin to add
     * @return Information about results of the operation
     */
    @PostMapping(value = "/admins/{uid:[\\w-:.]+}")
    public ResponseEntity<GroupingsServiceResult> addNewAdmin(@RequestHeader("current_user") String currentUser,
            @PathVariable String uid) {
        logger.info("Entered REST addNewAdmin...");
        return ResponseEntity
                .ok()
                .body(membershipService.addAdmin(currentUser, uid));
    }

    @PostMapping(value = "/newAdd/{path}/{uids}")
    public ResponseEntity<GenericServiceResult> addGroupMemberr(@RequestHeader("current_user") String currentUser,
            @PathVariable String path, @PathVariable List<String> uids) {
        return ResponseEntity
                .ok()
                .body(membershipService.addGroupMemberr(currentUser, path + INCLUDE, uids));
    }

    /**
     * Delete an admin
     *
     * @param uid: uid or uuid of admin to delete
     * @return Information about results of the operation
     */
    @DeleteMapping(value = "/admins/{uid:[\\w-:.]+}")
    public ResponseEntity<GroupingsServiceResult> deleteNewAdmin(@RequestHeader("current_user") String currentUser,
            @PathVariable String uid) {
        logger.info("Entered REST deleteNewAdmin...");
        return ResponseEntity
                .ok()
                .body(membershipService.deleteAdmin(currentUser, uid));
    }

    /**
     * Delete a user from multiple groupings
     *
     * @param paths: path of groupings to modify
     * @param uid:   uid or uuid of user to delete
     * @return Information about results of operation
     */
    @DeleteMapping(value = "/admins/{paths}/{uid}")
    public ResponseEntity<List<GroupingsServiceResult>> removeFromGroups(
            @RequestHeader("current_user") String currentUser,
            @PathVariable List<String> paths,
            @PathVariable String uid) {
        logger.info("Entered REST removeFromGroups...");
        return ResponseEntity
                .ok()
                .body(membershipService.removeFromGroups(currentUser, uid, paths));
    }

    /**
     * Get a member's attributes based off username or id number
     *
     * @param uid: Username or id number of user to obtain attributes about
     * @return Map of user attributes
     */
    @GetMapping(value = "/members/{uid:[\\w-:.<>]+}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> memberAttributes(@RequestHeader("current_user") String currentUser,
            @PathVariable String uid) {
        logger.info("Entered REST memberAttributes...");
        return ResponseEntity
                .ok()
                .body(memberAttributeService.getUserAttributes(currentUser, uid));
    }

    /**
     * Get a list of a groupings a user is in and can opt into
     * <p>
     * <<<<<<< HEAD
     *
     * @param path:        Path of specific grouping
     * @param page:        Page of grouping to retrieve (starts at 1)
     * @param size:        Size of page of grouping to retrieve
     * @param sortString:  Page of grouping to retrieve
     * @param isAscending: Page of grouping to retrieve (starts at 1)
     * @return Grouping found at specified path
     */
    @GetMapping(value = "/groupings/{path:[\\w-:.]+}")
    @ResponseBody
    public ResponseEntity<Grouping> getGrouping(@RequestHeader("current_user") String currentUser,
            @PathVariable String path,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortString,
            @RequestParam(required = false) Boolean isAscending) {
        logger.info("Entered REST getGrouping...");
        return ResponseEntity
                .ok()
                .body(groupingAssignmentService
                        .getPaginatedGrouping(path, currentUser, page, size, sortString, isAscending));
    }

    /**
     * Get a List of memberships as which uid has.
     */
    @GetMapping(value = "/members/{uid:[\\w-:.]+}/groupings")
    @ResponseBody
    public ResponseEntity<List<Membership>> membershipResults(@RequestHeader("current_user") String currentUser,
            @PathVariable String uid) {
        logger.info("Entered REST membershipResults...");
        return ResponseEntity
                .ok()
                .body(membershipService.getMembershipResults(currentUser, uid));
    }

    /**
     * Get a list of all the paths associated with the groupings which uid as the ability top opt into.
     */
    @GetMapping(value = "/groupings/optInGroups/{uid}")
    @ResponseBody
    public ResponseEntity<List<String>> getOptInGroups(@RequestHeader("current_user") String currentUser,
            @PathVariable String uid) {
        logger.info("Entered REST optInGroups...");
        return ResponseEntity
                .ok()
                .body(groupingAssignmentService
                        .getOptInGroups(currentUser, uid));
    }

    /**
     * if the user is allowed to opt into the grouping
     * this will add them to the include group of that grouping
     * if the user is in the exclude group, they will be removed from it
     *
     * @param path : the path to the grouping where the user will be opting in
     * @param uid  : the uid of the user that will be opted in
     * @return information about the success of opting in
     */
    @PutMapping(value = "/groupings/{path:[\\w-:.]+}/includeMembers/{uid:[\\w-:.]+}/self")
    public ResponseEntity<List<GroupingsServiceResult>> optIn(@RequestHeader("current_user") String currentUser,
            @PathVariable String path,
            @PathVariable String uid) {
        logger.info("Entered REST optIn...");
        return ResponseEntity
                .ok()
                .body(membershipService.optIn(currentUser, path, uid));
    }

    /**
     * if the user is allowed to opt out of the grouping
     * this will add them to the exclude group of that grouping
     * if the user is in the include group of that Grouping, they will be removed from it
     *
     * @param path : the path to the grouping where the user will be opting out
     * @param uid  : the uid of the user that will be opted out
     * @return information about the success of opting out
     */
    @PutMapping(value = "/groupings/{path:[\\w-:.]+}/excludeMembers/{uid:[\\w-:.]+}/self")
    public ResponseEntity<List<GroupingsServiceResult>> optOut(@RequestHeader("current_user") String currentUser,
            @PathVariable String path,
            @PathVariable String uid) {
        logger.info("Entered REST optOut...");
        return ResponseEntity
                .ok()
                .body(membershipService.optOut(currentUser, path, uid));
    }

    /**
     * Update grouping to add new include member
     *
     * @param path: path of grouping to update
     * @param uid:  uid or uuid of member to add to include
     * @return Information about results of the operation
     */
    @PutMapping(value = "/groupings/{path:[\\w-:.]+}/includeMembers/{uid:[\\w-:.]+}")
    public ResponseEntity<List<GroupingsServiceResult>> includeMembers(
            @RequestHeader("current_user") String currentUser, @PathVariable String path,
            @PathVariable String uid) {
        logger.info("Entered REST includeMembers...");
        path = path + INCLUDE;
        return ResponseEntity
                .ok()
                .body(membershipService.addGroupMember(currentUser, path, uid));
    }

    /**
     * Update grouping to add include multiple members
     *
     * @param path: path of grouping to update
     * @param uids: uids or uuids of members to add to include
     * @return Information about results of the operation
     */
    @PutMapping(value = "/groupings/{path:[\\w-:.]+}/includeMultipleMembers/{uids}")
    public ResponseEntity<List<GroupingsServiceResult>> includeMultipleMembers(
            @RequestHeader("current_user") String currentUser, @PathVariable String path,
            @PathVariable List<String> uids) throws IOException, MessagingException {
        logger.info("Entered REST includeMultipleMembers...");
        path = path + INCLUDE;
        return ResponseEntity
                .ok()
                .body(membershipService.addGroupMembers(currentUser, path, uids));
    }

    /**
     * Update grouping to add new exclude member
     *
     * @param path: path of grouping to update
     * @param uid:  uid or uuid of member to add to exclude
     * @return Information about results of the operation
     */
    @PutMapping(value = "/groupings/{path:[\\w-:.]+}/excludeMembers/{uid:[\\w-:.]+}")
    public ResponseEntity<List<GroupingsServiceResult>> excludeMembers(
            @RequestHeader("current_user") String currentUser, @PathVariable String path,
            @PathVariable String uid) {
        logger.info("Entered REST excludeMembers...");
        path = path + EXCLUDE;
        return ResponseEntity
                .ok()
                .body(membershipService.addGroupMember(currentUser, path, uid));
    }

    /**
     * Update grouping to add exclude multiple members
     *
     * @param path: path of grouping to update
     * @param uids: uids or uuids of members to add to exclude
     * @return Information about results of the operation
     */
    @PutMapping(value = "/groupings/{path:[\\w-:.]+}/excludeMultipleMembers/{uids}")
    public ResponseEntity<List<GroupingsServiceResult>> excludeMultipleMembers(
            @RequestHeader("current_user") String currentUser, @PathVariable String path,
            @PathVariable List<String> uids) throws IOException, MessagingException {
        logger.info("Entered REST excludeMultipleMembers...");
        path = path + EXCLUDE;
        return ResponseEntity
                .ok()
                .body(membershipService.addGroupMembers(currentUser, path, uids));
    }

    /**
     * Remove grouping include member
     *
     * @param path: path of grouping to modify
     * @param uid:  uid or uuid of grouping include member to remove
     * @return Information about results of operation
     */
    @DeleteMapping(value = "/groupings/{path:[\\w-:.]+}/includeMembers/{uid:[\\w-:.]+}")
    public ResponseEntity<GroupingsServiceResult> deleteInclude(@RequestHeader("current_user") String currentUser,
            @PathVariable String path,
            @PathVariable String uid) {
        logger.info("Entered REST deleteInclude");
        return ResponseEntity
                .ok()
                .body(membershipService.deleteGroupMember(currentUser, path + INCLUDE, uid));
    }

    /**
     * Remove grouping exclude member
     *
     * @param path: path of grouping to modify
     * @param uid:  uid or uuid of grouping exclude member to remove
     * @return Information about results of operation
     */
    @DeleteMapping(value = "/groupings/{path:[\\w-:.]+}/excludeMembers/{uid:[\\w-:.]+}")
    public ResponseEntity<GroupingsServiceResult> deleteExclude(@RequestHeader("current_user") String currentUser,
            @PathVariable String path,
            @PathVariable String uid) {
        logger.info("Entered REST deleteExclude");
        return ResponseEntity
                .ok()
                .body(membershipService.deleteGroupMember(currentUser, path + EXCLUDE, uid));
    }

    /**
     * Delete all valid members in uids from path as currentUser.
     *
     * @param currentUser
     * @param path
     * @param uids
     * @return
     */
    @DeleteMapping(value = "/groupings/{path:[\\w-:.]+}/excludeMultipleMembers/{uids}")
    public ResponseEntity<List<GroupingsServiceResult>> deleteMultipleExcludeMembers(
            @RequestHeader("current_user") String currentUser,
            @PathVariable String path,
            @PathVariable List<String> uids) {
        logger.info("Entered REST deleteExclude");
        return ResponseEntity
                .ok()
                .body(membershipService.deleteGroupMembers(currentUser, path + EXCLUDE, uids));
    }

    /**
     * Get an owner's owned groupings by username or UH id number
     *
     * @param uid: Username of owner to get list of groupings they own
     * @return List of owner's owned groupings
     */
    @GetMapping("/owners/{uid:[\\w-:.]+}/groupings")
    public ResponseEntity<List<Grouping>> ownerGroupings(@RequestHeader("current_user") String currentUser,
            @PathVariable String uid) {
        logger.info("Entered REST ownerGroupings...");
        return ResponseEntity
                .ok()
                .body(groupingAssignmentService.restGroupingsOwned(currentUser, uid));
    }

    /**
     * Update grouping to add a new owner
     *
     * @param path: path of grouping to update
     * @param uid:  uid/uuid of new owner to add
     * @return Information about results of operation
     */
    @PutMapping(value = "/groupings/{path:[\\w-:.]+}/owners/{uid:[\\w-:.]+}")
    public ResponseEntity<GroupingsServiceResult> addOwner(@RequestHeader("current_user") String currentUser,
            @PathVariable String path,
            @PathVariable String uid) {
        logger.info("Entered REST addOwner...");
        return ResponseEntity
                .ok()
                .body(memberAttributeService.assignOwnership(path, currentUser, uid));
    }

    /**
     * Delete a grouping owner
     *
     * @param path: path of grouping to modify
     * @param uid:  uid or uuid of owner to delete
     * @return Information about results of operation
     */
    @DeleteMapping(value = "/groupings/{path:[\\w-:.]+}/owners/{uid:[\\w-:.]+}")
    public ResponseEntity<GroupingsServiceResult> deleteOwner(@RequestHeader("current_user") String currentUser,
            @PathVariable String path,
            @PathVariable String uid) {
        logger.info("Entered REST deleteOwner");
        return ResponseEntity
                .ok()
                .body(memberAttributeService.removeOwnership(path, currentUser, uid));
    }

    /**
     * Update grouping description
     *
     * @param path:      path of grouping to update
     * @param dtoString: new description to be updated
     * @return Information about results of operation
     */
    @PutMapping(value = "/groupings/{path:[\\w-:.]+}/description")
    public ResponseEntity<GroupingsServiceResult> updateDescription(
            @RequestHeader("current_user") String currentUser,
            @PathVariable String path,
            @RequestBody(required = false) String dtoString) {
        logger.info("Entered REST updateDescription");
        return ResponseEntity
                .ok()
                .body(groupAttributeService.updateDescription(path, currentUser, dtoString));
    }

    /**
     * Update grouping to enable given preference
     *
     * @param path:         path of grouping to update
     * @param syncDestName: name of syncDest to update
     * @return Information about result of operation
     */
    @PutMapping(value = "/groupings/{path:[\\w-:.]+}/syncDests/{syncDestName:[\\w-:.]+}/enable")
    public ResponseEntity<GroupingsServiceResult> enableSyncDest(
            @RequestHeader("current_user") String currentUser,
            @PathVariable String path,
            @PathVariable String syncDestName) {
        return ResponseEntity
                .ok()
                .body(groupAttributeService.changeGroupAttributeStatus(path, currentUser, syncDestName, true));
    }

    /**
     * Update grouping to disable given preference
     *
     * @param path:         path of grouping to update
     * @param syncDestName: name of syncDest to update
     * @return Information about result of operation
     */
    @PutMapping(value = "/groupings/{path:[\\w-:.]+}/syncDests/{syncDestName:[\\w-:.]+}/disable")
    public ResponseEntity<GroupingsServiceResult> disableSyncDest(
            @RequestHeader("current_user") String currentUser,
            @PathVariable String path,
            @PathVariable String syncDestName) {
        return ResponseEntity
                .ok()
                .body(groupAttributeService.changeGroupAttributeStatus(path, currentUser, syncDestName, false));
    }

    /*
    @RequestMapping(value = "/admins/{uid:[\\w-:.]+}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Membership>> getMembershipResults(
            @RequestHeader("current_user") String currentUser,
            @PathVariable String uid) {
        logger.info("Entered REST checkInBasis");
        return ResponseEntity
                .ok()
                .body(membershipService.getMemberShipResults(currentUser, uid));
    }
    */

    /**
     * GET a response which specifies whether uid is an owner or not,
     */
    @GetMapping(value = "/admins/{uid:[\\w-:.]+}")
    @ResponseBody
    public ResponseEntity<GenericServiceResult> getIsAdmin(@RequestHeader("current_user") String currentUser,
            @PathVariable String uid) {
        logger.info("Entered REST getAllSyncDestinations...");
        return ResponseEntity
                .ok()
                .body(memberAttributeService.getIsAdmin(currentUser, uid));
    }

    /**
     * Update grouping to enable given preference
     *
     * @param path:         path of grouping to update
     * @param preferenceId: id of preference to update
     * @return Information about result of operation
     */
    @RequestMapping(value = "/groupings/{path:[\\w-:.]+}/preferences/{preferenceId:[\\w-:.]+}/enable",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<GroupingsServiceResult>> enablePreference(
            @RequestHeader("current_user") String currentUser,
            @PathVariable String path,
            @PathVariable String preferenceId) {
        logger.info("Entered REST enablePreference");
        List<GroupingsServiceResult> results = new ArrayList<>();

        if (OPT_IN.equals(preferenceId))
            results = groupAttributeService.changeOptInStatus(path, currentUser, true);
        else if (OPT_OUT.equals(preferenceId))
            results = groupAttributeService.changeOptOutStatus(path, currentUser, true);
        else
            throw new UnsupportedOperationException();

        return ResponseEntity
                .ok()
                .body(results);
    }

    /**
     * Update grouping to disable given preference
     *
     * @param path:         path of grouping to update
     * @param preferenceId: id of preference to update
     * @return Information about result of operation
     */
    @RequestMapping(value = "/groupings/{path:[\\w-:.]+}/preferences/{preferenceId:[\\w-:.]+}/disable",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<GroupingsServiceResult>> disablePreference(
            @RequestHeader("current_user") String currentUser,
            @PathVariable String path,
            @PathVariable String preferenceId) {
        logger.info("Entered REST disablePreference");
        List<GroupingsServiceResult> results = new ArrayList<>();

        if (OPT_IN.equals(preferenceId))
            results = groupAttributeService.changeOptInStatus(path, currentUser, false);
        else if (OPT_OUT.equals(preferenceId))
            results = groupAttributeService.changeOptOutStatus(path, currentUser, false);
        else
            throw new UnsupportedOperationException();
        return ResponseEntity
                .ok()
                .body(results);
    }

    /**
     * Get the list of sync destinations
     */
    @RequestMapping(value = "/groupings/{path:[\\w-:.]+}/syncDestinations",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<SyncDestination>> getSyncDestinations(@RequestHeader("current_user") String
            currentUser,
            @PathVariable String path) throws Exception {
        logger.info("Entered REST getAllSyncDestinations...");
        return ResponseEntity
                .ok()
                .body(groupAttributeService.getAllSyncDestinations(currentUser, path));
    }

    /**
     * GET a response which specifies whether uid is an owner or not,
     */
    @RequestMapping(value = "/owners/{uid:[\\w-:.]+}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<GenericServiceResult> getIsOwner(@RequestHeader("current_user") String currentUser,
            @PathVariable String uid) {
        logger.info("Entered REST getAllSyncDestinations...");
        return ResponseEntity
                .ok()
                .body(memberAttributeService.getIsOwner(currentUser, uid));
    }

}
