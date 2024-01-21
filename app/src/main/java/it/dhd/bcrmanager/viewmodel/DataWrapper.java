package it.dhd.bcrmanager.viewmodel;

import java.util.List;

import it.dhd.bcrmanager.objects.ContactItem;

public record DataWrapper (List<Object> sortedListWithHeaders,
                           List<Object> starredListWithHeader,
                           List<ContactItem> contactList,
                           List<String> errorFiles,
                           double maxDuration) {}
