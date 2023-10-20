import FormModal from "../../../components/modal/FormModal";
import useCreateSubject from "./useCreateSubject";
import useUpdateSubject from "./useUpdateSubject";
import SubjectForm from "./SubjectForm";
import {ModalOpenMode} from "../../../utils/constants";



const SubjectModal = ({open, subject, onClose, modalOpenMode, submitButton, ...props}) => {
    const {mutate: onCreate} = useCreateSubject();
    const {mutate: onUpdate} = useUpdateSubject();

    const initialValues = {
        name: subject.name? subject.name : "",
        teacher: subject.teacher? subject.teacher : "",
        id: subject.id? subject.id : 0
    };

    return (
        <FormModal
            open={open}
            title={"დამატება"}
            cancelText={'დახურვა'}
            saveText={'შენახვა'}
            width={700}
            height={250}
            maxWidth={700}
            maxHeight={250}
            initialValues={

                {
                    ...initialValues,
                }
            }
            formProps={{
                modalOpenMode
            }}
            form={SubjectForm}
            // validationSchema={validateNewsFormData(!!news, t)}
            onSubmit={modalOpenMode === ModalOpenMode.add ? onCreate : onUpdate}
            onClose={onClose}
            submitButton={submitButton}
            {...props}
        />
    );
};

export default SubjectModal;