// This is a service worker, but instead of being used
// for caching resources for using the web page offline, this
// service worker is used to create a fake local web server. 
// This fake local web server will serve locally-stored developer
// content as if it came from a web server. This allows people
// to develop web resources and web programs locally without needing
// to send data to a server.

self.addEventListener('activate', (event) => {});
self.addEventListener('install', (event) => {
	self.skipWaiting();
});

// Listen for accesses to resources
self.addEventListener('fetch', (event) => {
	// Relativize the fetch url
	if (event.request.url.startsWith(registration.scope)) {
		var relativeUrl = event.request.url.substring(registration.scope.length);
		// Just to be consistent, strip any initial '/' symbols, even though they aren't expected
		if (relativeUrl.length > 0 && relativeUrl.charAt(0) == '/')
			relativeUrl = relativeUrl.substring(1);
		// See if we're accessing the special paths where we have our fake web server
		var localMatch = relativeUrl.match('^test([^/]*)/(.*$)'); 
		if (localMatch) {
			// Special handling of URLs in a certain path where we'll
			// serve them special local content
			var testSwitch = localMatch[1];
			var testPath = localMatch[2]; 
			console.log(testSwitch);
			console.log(testPath);
			event.respondWith(new Response('No resource found with that name', 
				{status: 404}));
		} else {
			// For everything else, just fall through and let the default
			// handling occur
		}
	}
	// Don't know what to do, just let the default handling happen	
});