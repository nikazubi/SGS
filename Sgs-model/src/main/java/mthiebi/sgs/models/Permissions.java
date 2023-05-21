package mthiebi.sgs.models;

public enum Permissions {
	SYSTEM_PARAMETERS_VIEW("systemParameters"),
	SYSTEM_PARAMETERS_MANAGE("systemParameters"),
	USERS_VIEW("users"),
	USERS_MANAGE("users"),
	USERS_GROUPS_VIEW("userGroups"),
	USERS_GROUPS_MANAGE("userGroups"),
	CHANNELS_VIEW("channels"),
	CHANNELS_MANAGE("channels"),
	SERVICES_VIEW("services"),
	SERVICES_MANAGE("services"),
	CLIENT_COMMISSIONS_VIEW("commissions"),
	CLIENT_COMMISSIONS_MANAGE("commissions"),
	PAYMENTS_VIEW("payments"),
	PAYMENTS_PERFORM("payments");

	private final String permissionsGroup;

	public String getPermissionsGroup() {
		return permissionsGroup;
	}

	Permissions(String permissionsGroup) {
		this.permissionsGroup = permissionsGroup;
	}
}
