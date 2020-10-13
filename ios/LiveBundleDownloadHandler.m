#import "LiveBundle.h"
#import "SSZipArchive.h"

NSString *const liveBundle_zip = @"LB-Bundle.zip";
NSString *const liveBundle_js = @"LB-Bundle.js";

@implementation LiveBundleDownloadHandler {
}

- (id)init:(NSString *)downloadFilePath
operationQueue:(dispatch_queue_t)operationQueue
progressCallback:(void (^)(long long, long long))progressCallback
doneCallback:(void (^)(BOOL))doneCallback
failCallback:(void (^)(NSError *err))failCallback {
    self.doneCallback = doneCallback;
    self.failCallback = failCallback;
    return self;
}
+ (void)download:(NSURL *)url bundleId:(NSString *) bundleId completion:(void (^)(NSString *filepath))completion {
    dispatch_queue_t queue = dispatch_get_global_queue(0,0);
    dispatch_async(queue, ^{
        NSData *urlData = [NSData dataWithContentsOfURL:url];
        //NSString *path = [[NSBundle mainBundle] bundlePath];
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
        NSString *path = [paths objectAtIndex:0];
        //Save the data
        NSString *zipPath = [path stringByAppendingPathComponent:liveBundle_zip];
        NSString *jsPath = [path stringByAppendingPathComponent:liveBundle_js];
        NSString *liveBundlePath = [path stringByAppendingPathComponent:bundleId];
        zipPath = [zipPath stringByStandardizingPath];
        jsPath = [jsPath stringByStandardizingPath];
        NSError *error = nil;
        liveBundlePath = [liveBundlePath stringByStandardizingPath];
        [urlData writeToFile:zipPath atomically:YES];
        [SSZipArchive unzipFileAtPath:zipPath toDestination:path];
        if ([[NSFileManager defaultManager] fileExistsAtPath:jsPath]) {
            [[NSFileManager defaultManager] removeItemAtPath:jsPath error:&error];
        }
        [[NSFileManager defaultManager] moveItemAtPath:liveBundlePath toPath:jsPath error:&error];
        [[NSFileManager defaultManager] removeItemAtPath:zipPath error:&error];
        completion(jsPath);
    });
}
@end
