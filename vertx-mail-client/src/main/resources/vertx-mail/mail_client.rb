require 'vertx/vertx'
require 'vertx/util/utils.rb'
# Generated from io.vertx.ext.mail.MailClient
module VertxMail
  #  SMTP mail client for Vert.x
  #  <p>
  #  A simple asynchronous API for sending mails from Vert.x applications
  class MailClient
    # @private
    # @param j_del [::VertxMail::MailClient] the java delegate
    def initialize(j_del)
      @j_del = j_del
    end
    # @private
    # @return [::VertxMail::MailClient] the underlying java delegate
    def j_del
      @j_del
    end
    #  create a non shared instance of the mail client
    # @param [::Vertx::Vertx] vertx the Vertx instance the operation will be run in
    # @param [Hash] config MailConfig configuration to be used for sending mails
    # @return [::VertxMail::MailClient] MailClient instance that can then be used to send multiple mails
    def self.create_non_shared(vertx=nil,config=nil)
      if vertx.class.method_defined?(:j_del) && config.class == Hash && !block_given?
        return ::Vertx::Util::Utils.safe_create(Java::IoVertxExtMail::MailClient.java_method(:createNonShared, [Java::IoVertxCore::Vertx.java_class,Java::IoVertxExtMail::MailConfig.java_class]).call(vertx.j_del,Java::IoVertxExtMail::MailConfig.new(::Vertx::Util::Utils.to_json_object(config))),::VertxMail::MailClient)
      end
      raise ArgumentError, "Invalid arguments when calling create_non_shared(vertx,config)"
    end
    #  Create a Mail client which shares its data source with any other Mongo clients created with the same
    #  pool name
    # @param [::Vertx::Vertx] vertx the Vert.x instance
    # @param [Hash] config the configuration
    # @param [String] poolName the pool name
    # @return [::VertxMail::MailClient] the client
    def self.create_shared(vertx=nil,config=nil,poolName=nil)
      if vertx.class.method_defined?(:j_del) && config.class == Hash && !block_given? && poolName == nil
        return ::Vertx::Util::Utils.safe_create(Java::IoVertxExtMail::MailClient.java_method(:createShared, [Java::IoVertxCore::Vertx.java_class,Java::IoVertxExtMail::MailConfig.java_class]).call(vertx.j_del,Java::IoVertxExtMail::MailConfig.new(::Vertx::Util::Utils.to_json_object(config))),::VertxMail::MailClient)
      elsif vertx.class.method_defined?(:j_del) && config.class == Hash && poolName.class == String && !block_given?
        return ::Vertx::Util::Utils.safe_create(Java::IoVertxExtMail::MailClient.java_method(:createShared, [Java::IoVertxCore::Vertx.java_class,Java::IoVertxExtMail::MailConfig.java_class,Java::java.lang.String.java_class]).call(vertx.j_del,Java::IoVertxExtMail::MailConfig.new(::Vertx::Util::Utils.to_json_object(config)),poolName),::VertxMail::MailClient)
      end
      raise ArgumentError, "Invalid arguments when calling create_shared(vertx,config,poolName)"
    end
    #  send a single mail via MailClient
    # @param [Hash] email MailMessage object containing the mail text, from/to, attachments etc
    # @yield will be called when the operation is finished or it fails (may be null to ignore the result)
    # @return [self]
    def send_mail(email=nil)
      if email.class == Hash && block_given?
        @j_del.java_method(:sendMail, [Java::IoVertxExtMail::MailMessage.java_class,Java::IoVertxCore::Handler.java_class]).call(Java::IoVertxExtMail::MailMessage.new(::Vertx::Util::Utils.to_json_object(email)),(Proc.new { |ar| yield(ar.failed ? ar.cause : nil, ar.succeeded ? ar.result != nil ? JSON.parse(ar.result.toJson.encode) : nil : nil) }))
        return self
      end
      raise ArgumentError, "Invalid arguments when calling send_mail(email)"
    end
    #  close the MailClient
    # @return [void]
    def close
      if !block_given?
        return @j_del.java_method(:close, []).call()
      end
      raise ArgumentError, "Invalid arguments when calling close()"
    end
  end
end
