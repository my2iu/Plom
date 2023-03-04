// This is a service worker, but instead of being used
// for caching resources for using the web page offline, this
// service worker is used to create a fake local web server. 
// This fake local web server will serve locally-stored developer
// content as if it came from a web server. This allows people
// to develop web resources and web programs locally without needing
// to send data to a server.
//

// The general strategy that the service worker uses is
//   1. wait for incoming requests for resources
//   2. send out a broadcast request to see if anyone has data for the resources
//   3. page with the IDE will send a message back to the service worker with the data for the resources
//   4. resend that data as a response to the request
// So the service worker has no smarts or storage. It just relays
// requests to everyone, and hopes that someone will reply with the 
// needed data  



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
			event.respondWith(new Promise((resolve, reject) => {
				// Queue up a promise that will resolve later after we've
				// asked the main code page to provide the data for the 
				// requested resource
				pendingResourceRequests[testSwitch + '/' + testPath] = resolve;	
				// Ask if anyone has the data for the resource being requested
				requestResourceChannel.postMessage({
					type:'GET',
					serverContext: testSwitch,
					path: testPath
				})
				
//				new Response('No resource found with that name', 
//					{status: 404}));
			}));
				
		} else {
			// For everything else, just fall through and let the default
			// handling occur
		}
	}
	// Don't know what to do, just let the default handling happen	
});

// We use a broadcast channel for talking to the main page that actually has
// the pages and resources that we're serving from this virtual web server.
// We can't use a more direct MessageChannel because the service worker
// can often be killed, destroying the MessageChannel connection.
var requestResourceChannel = new BroadcastChannel('Plom virtual web server service worker request resource');

// Holds pending requests to other threads/pages for resources. Since 
// service workers can often be terminated, this global variable might
// be erased, but in those cases, the underlying pending requests will
// have probably expired too. The pending requests are stored as the
// 'resolve' function of a promise that is expected a fetch response.
var pendingResourceRequests = {};

// When we send out broadcast requests for the data, we receive responses
// here, and we can then serve the data 
self.addEventListener('message', (event) => {
	// Expected format: {
	//    type: 'GET',
	//    serverContext: 'identifying id of server,
	//    path: 'index.html',
	//    [headers: {name:value}]  optional key-value pairs to use as headers 
	//    data: arraybuffer of data or null for a 404
	// }
	if (event.data.type == 'GET') {
		var resolve = pendingResourceRequests[event.data.serverContext + '/' + event.data.path];
		if (!resolve) {
			// No request was made for the received data, or the request
			// took so long that the service worker was killed and lost the
			// the request
			return;
		};
		if (event.data.data) {
			if (event.data.headers)
				resolve(new Response(event.data.data), {status: 200, headers: event.data.headers});
			else
				resolve(new Response(event.data.data));
		} else {
			if (event.data.headers)
				resolve(new Response('No resource found with that name', 
					{status: 404, headers: event.data.headers}));
			else
				resolve(new Response('No resource found with that name', 
					{status: 404}));
		}
	}
});