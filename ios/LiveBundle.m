#import "LiveBundle.h"
#import "SSZipArchive.h"
#import <React/RCTBridge+Private.h>
#import <React/RCTBundleURLProvider.h>
#import <UIKit/UIKit.h>
#import <React/RCTReloadCommand.h>
#import <React/RCTBundleURLProvider.h>

#if __has_include(<React/RCTRootView.h>)
#import <React/RCTRootView.h>
#elif __has_include("RCTRootView.h")
#import "RCTRootView.h"
#else
#import "React/RCTRootView.h"   // Required when used as a Pod in a Swift project
#endif

NSString *const liveBundle_zip = @"LB-Bundle.zip";
NSString *const liveBundle_js = @"LB-Bundle.js";

@interface LiveBundle ()
@end

@implementation LiveBundle
static NSString *sStorageUrlSuffix;
static NSString *sStorageUrl;
static NSString *bundleId = @"";
static NSString *packageId = @"";
static BOOL isBundleInstalled = NO;
static BOOL isSessionStarted = NO;
static BOOL sIsInitialLaunch = YES;
static NSString *urlString = @"";
@synthesize bridge = _bridge;

- (instancetype)initWithstorageUrl:(NSString*)storageUrl storageUrSulffix:(NSString*)storageUrlSuffix {
    if (self = [super init]) {
        sStorageUrl = [storageUrl stringByStandardizingPath];
        sStorageUrlSuffix = storageUrlSuffix;
    }
    return self;
}

RCT_EXPORT_MODULE()
- (NSDictionary *)constantsToExport {
         return @{ @"STORAGE_URL" : sStorageUrl,
                   @"STORAGE_URL_SUFFIX" : sStorageUrlSuffix,
                   @"PACKAGE_ID" : packageId,
                   @"BUNDLE_ID": bundleId,
                   @"IS_BUNDLE_INSTALLED": [NSNumber numberWithBool:isBundleInstalled],
                   @"IS_SESSION_STARTED": [NSNumber numberWithBool:isSessionStarted],
                   @"IS_INITIAL_LAUNCH": [NSNumber numberWithBool:sIsInitialLaunch]
              };
}

+ (BOOL)requiresMainQueueSetup
{
  return YES;
}

- (void)setBridge:(RCTBridge *)bridge
{
  _bridge = bridge;
}

RCT_EXPORT_METHOD(getState:(RCTPromiseResolveBlock)resolve reject:(__unused RCTPromiseRejectBlock)reject) {
    NSDictionary *state = @{@"isBundleInstalled": [NSNumber numberWithBool:isBundleInstalled], @"isSessionStarted": [NSNumber numberWithBool:isSessionStarted], @"packageId": packageId, @"bundleId": bundleId};
    resolve(@[state]);
}

RCT_EXPORT_METHOD(launchUI:(NSDictionary *)props resolve:(RCTPromiseResolveBlock)resolve reject:(__unused RCTPromiseRejectBlock)reject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (![urlString  isEqual: @""]) {
        NSURL *bundleURL = [[NSURL alloc] initFileURLWithPath:urlString];
        [self->_bridge setBundleURL:bundleURL];
    }
        RCTRootView *rootView = [[RCTRootView alloc] initWithBridge:self->_bridge
                                                         moduleName:@"LiveBundleUI"
                                                  initialProperties:props];
        UIViewController * vc = [[UIViewController alloc] init];
        vc.view = rootView;
        UINavigationController *rootVC = (UINavigationController*)[[[UIApplication sharedApplication] keyWindow] rootViewController];
        [rootVC pushViewController:vc animated:true];
        sIsInitialLaunch = NO;
        resolve(nil);
    });
}

RCT_EXPORT_METHOD(reset:(RCTPromiseResolveBlock)resolve reject:(__unused RCTPromiseRejectBlock)reject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        packageId = @"";
        bundleId = @"";
        isBundleInstalled = NO;
        isSessionStarted = NO;
        urlString = @"";
        NSURL *url;
#if DEBUG
        url = [[RCTBundleURLProvider sharedSettings] jsBundleURLForBundleRoot:@"index" fallbackResource:nil];
#else
        url = [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];
#endif
        [self->_bridge setBundleURL: url];
        UINavigationController *rootVC = (UINavigationController*)[[[UIApplication sharedApplication] keyWindow] rootViewController];
        [rootVC popViewControllerAnimated:NO];
        RCTTriggerReloadCommandListeners(@"reset bridge");
        resolve(nil);
    });
}

RCT_EXPORT_METHOD(launchLiveSession:(NSString *)serverHost resolve:(RCTPromiseResolveBlock)resolve reject:(__unused RCTPromiseRejectBlock)reject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        urlString = [NSString stringWithFormat:@"http://%@/index.bundle?platform=ios&dev=false&minify=true", serverHost];
        NSURL *url = [[NSURL alloc] initWithString:urlString];
        [self->_bridge setBundleURL: url];
        UINavigationController *rootVC = (UINavigationController*)[[[UIApplication sharedApplication] keyWindow] rootViewController];
        [rootVC popViewControllerAnimated:NO];
        isSessionStarted = YES;
        RCTTriggerReloadCommandListeners(@"launch Live Session");
        resolve(nil);
    });
}

- (void)launchUI {
    __strong RCTBridge *strongBridge = self.bridge;
    strongBridge.bundleURL = [[RCTBundleURLProvider sharedSettings] jsBundleURLForBundleRoot:@"index" fallbackResource:nil];
    RCTRootView *rootView = [[RCTRootView alloc] initWithBridge:strongBridge
                                                     moduleName:@"LiveBundleUI"
                                              initialProperties:nil];
    UIViewController * vc = [[UIViewController alloc] init];
    vc.view = rootView;
    UINavigationController *rootVC = (UINavigationController*)[[[UIApplication sharedApplication] keyWindow] rootViewController];
    [rootVC pushViewController:vc animated:true];
}

- (void)installBundle {
    dispatch_async(dispatch_get_main_queue(), ^{
        UINavigationController *rootVC = (UINavigationController*)[[[UIApplication sharedApplication] keyWindow] rootViewController];
        [rootVC popViewControllerAnimated:NO];
        if (![urlString  isEqual: @""]) {
            NSURL *bundleURL = [[NSURL alloc] initFileURLWithPath:urlString];
            [self->_bridge setBundleURL:bundleURL];
        }
        RCTTriggerReloadCommandListeners(@"install new bundle");
        });
}

RCT_EXPORT_METHOD(downloadBundle:(NSString *)pId bundleId: (NSString *)bId resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
    NSString *packageUrlString = [NSString stringWithFormat:@"%@/packages/%@/%@%@", sStorageUrl, pId, bId, sStorageUrlSuffix];
    NSURL *url = [NSURL URLWithString:packageUrlString];
    bundleId = bId;
    packageId = pId;
    dispatch_queue_t queue = dispatch_get_global_queue(0,0);
    dispatch_async(queue, ^{
        NSData *urlData = [NSData dataWithContentsOfURL:url];
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString *path = [paths objectAtIndex:0];
        //Save the data
        NSString *zipPath = [path stringByAppendingPathComponent:liveBundle_zip];
        urlString = [path stringByAppendingPathComponent:liveBundle_js];
        NSString *liveBundlePath = [path stringByAppendingPathComponent:bundleId];
        zipPath = [zipPath stringByStandardizingPath];
        urlString = [urlString stringByStandardizingPath];
        NSError *error = nil;
        liveBundlePath = [liveBundlePath stringByStandardizingPath];
        [urlData writeToFile:zipPath atomically:YES];
        [SSZipArchive unzipFileAtPath:zipPath toDestination:path];
        if ([[NSFileManager defaultManager] fileExistsAtPath:urlString]) {
            [[NSFileManager defaultManager] removeItemAtPath:urlString error:&error];
        }
        [[NSFileManager defaultManager] moveItemAtPath:liveBundlePath toPath:urlString error:&error];
        [[NSFileManager defaultManager] removeItemAtPath:zipPath error:&error];
        resolve(nil);
    });
}

RCT_EXPORT_METHOD(installBundle:(RCTPromiseResolveBlock)resolve reject:(__unused RCTPromiseRejectBlock)reject) {
    NSLog(@"installBundle");
    isBundleInstalled = YES;
    [self installBundle];
}
@end
